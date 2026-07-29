package com.sprint.mission.discodeit.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.request.channel.ChannelUpdateRequest;
import com.sprint.mission.discodeit.dto.request.channel.PrivateChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.channel.PublicChannelCreateRequest;
import com.sprint.mission.discodeit.dto.request.message.MessageCreateRequest;
import com.sprint.mission.discodeit.dto.request.message.MessageUpdateRequest;
import com.sprint.mission.discodeit.dto.request.readStatus.ReadStatusUpdateRequest;
import com.sprint.mission.discodeit.dto.request.user.UserCreateRequest;
import com.sprint.mission.discodeit.dto.request.user.UserLoginRequest;
import com.sprint.mission.discodeit.dto.request.user.UserUpdateRequest;
import com.sprint.mission.discodeit.dto.request.userStatus.UserStatusUpdateRequest;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.entity.Message;
import com.sprint.mission.discodeit.entity.MessageFile;
import com.sprint.mission.discodeit.entity.ReadStatus;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.entity.UserStatus;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.repository.MessageFileRepository;
import com.sprint.mission.discodeit.repository.MessageRepository;
import com.sprint.mission.discodeit.repository.ReadStatusRepository;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.repository.UserStatusRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@DisplayName("Discodeit API 통합 테스트")
class DiscodeitApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserStatusRepository userStatusRepository;

    @Autowired
    ChannelRepository channelRepository;

    @Autowired
    MessageRepository messageRepository;

    @Autowired
    MessageFileRepository messageFileRepository;

    @Autowired
    BinaryContentRepository binaryContentRepository;

    @Autowired
    ReadStatusRepository readStatusRepository;

    @Autowired
    EntityManager em;

    @Test
    @DisplayName("사용자 생성, 로그인, 상태 수정 통합 성공 - 실제 DB에 사용자와 상태가 반영")
    void userCreateLoginAndStatusUpdate_flowPersistsUserAndStatus() throws Exception {
        // given
        // 통합 테스트는 Controller 슬라이스 테스트와 달리 Service, Repository, Mapper, DB를 모두 실제 Bean으로 사용한다.
        // 이 테스트는 사용자 생성 요청이 User, BinaryContent, UserStatus 저장까지 이어지고,
        // 이후 로그인과 상태 수정 API가 같은 사용자 상태 row를 갱신하는지 확인한다.
        String suffix = uniqueSuffix();
        UserCreateRequest createRequest = new UserCreateRequest(
                "integrationUser-" + suffix,
                "integrationPassword",
                "integration-" + suffix + "@gmail.com"
        );
        MockMultipartFile userCreateRequestPart = jsonPart("userCreateRequest", createRequest);
        MockMultipartFile profilePart = filePart(
                "profile",
                "profile-" + suffix + ".png",
                MediaType.IMAGE_PNG_VALUE,
                "profile-image"
        );

        // when
        // 실제 multipart 사용자 생성 API를 호출한다.
        MvcResult createResult = mockMvc.perform(multipart("/api/users")
                        .file(userCreateRequestPart)
                        .file(profilePart)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                // HTTP 응답은 201 Created이며, 생성된 사용자와 프로필 메타데이터를 JSON으로 내려줘야 한다.
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.username").value(createRequest.username()))
                .andExpect(jsonPath("$.email").value(createRequest.email()))
                .andExpect(jsonPath("$.profile.id").exists())
                .andExpect(jsonPath("$.profile.fileName").value(profilePart.getOriginalFilename()))
                .andExpect(jsonPath("$.profile.size").value(profilePart.getSize()))
                .andExpect(jsonPath("$.profile.contentType").value(profilePart.getContentType()))
                .andExpect(jsonPath("$.online").value(true))
                .andReturn();

        JsonNode createBody = readBody(createResult);
        UUID userId = uuidAt(createBody, "/id");
        UUID profileId = uuidAt(createBody, "/profile/id");

        // 실제 DB 저장 여부를 Repository 재조회로 확인한다.
        // 영속성 컨텍스트를 비워 API 호출 결과가 1차 캐시가 아니라 DB에 flush된 상태인지 확인한다.
        flushAndClear();
        User savedUser = userRepository.findById(userId).orElseThrow(AssertionError::new);
        UserStatus savedStatus = userStatusRepository.findByUserId(userId).orElseThrow(AssertionError::new);

        assertThat(savedUser.getUsername()).isEqualTo(createRequest.username());
        assertThat(savedUser.getEmail()).isEqualTo(createRequest.email());
        assertThat(savedUser.getProfileId()).isEqualTo(profileId);
        assertThat(savedStatus.getUserId()).isEqualTo(userId);
        assertThat(binaryContentRepository.findById(profileId)).isPresent();

        // when
        // 생성한 사용자 계정으로 실제 로그인 API를 호출한다.
        UserLoginRequest loginRequest = new UserLoginRequest(
                createRequest.username(),
                createRequest.password()
        );
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))

                // then
                // AuthService, UserReader, UserStatusService, UserMapper가 함께 동작해 로그인 응답을 내려줘야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId.toString()))
                .andExpect(jsonPath("$.username").value(createRequest.username()))
                .andExpect(jsonPath("$.online").value(true));

        // when
        // 사용자 상태 수정 API로 lastActiveAt을 명시적으로 갱신한다.
        Instant updatedLastActiveAt = Instant.parse("2026-07-28T01:40:30Z");
        UserStatusUpdateRequest statusUpdateRequest = new UserStatusUpdateRequest(updatedLastActiveAt);
        mockMvc.perform(patch("/api/users/{userId}/userStatus", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusUpdateRequest)))

                // then
                // 상태 수정 응답은 같은 userId를 포함해야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(userId.toString()))
                .andExpect(jsonPath("$.lastActiveAt").exists());

        // DB에 저장된 UserStatus.lastActiveAt이 요청값으로 갱신됐는지 확인한다.
        flushAndClear();
        UserStatus updatedStatus = userStatusRepository.findByUserId(userId).orElseThrow(AssertionError::new);
        assertThat(updatedStatus.getLastActiveAt()).isEqualTo(updatedLastActiveAt);
    }

    @Test
    @DisplayName("공개 채널, 메시지, 첨부 파일 통합 성공 - 메시지 저장 후 목록 조회와 다운로드 가능")
    void publicChannelMessageAndAttachment_flowPersistsAndDownloadsAttachment() throws Exception {
        // given
        // 실제 사용자와 공개 채널을 API로 만든 뒤, 그 사용자와 채널을 참조하는 메시지를 첨부 파일과 함께 생성한다.
        // 이 흐름은 Controller, Service, Repository, Mapper, Querydsl 목록 조회, 로컬 파일 스토리지를 함께 검증한다.
        String suffix = uniqueSuffix();
        UUID authorId = createUser("messageAuthor-" + suffix, "message-author-" + suffix + "@gmail.com");

        PublicChannelCreateRequest channelCreateRequest = new PublicChannelCreateRequest(
                "public-channel-" + suffix,
                "integration public channel"
        );
        MvcResult channelCreateResult = mockMvc.perform(post("/api/channels/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(channelCreateRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value(ChannelType.PUBLIC.name()))
                .andExpect(jsonPath("$.name").value(channelCreateRequest.name()))
                .andReturn();

        UUID channelId = uuidAt(readBody(channelCreateResult), "/id");

        MessageCreateRequest messageCreateRequest = new MessageCreateRequest(
                "integration message",
                channelId,
                authorId
        );
        MockMultipartFile messageCreateRequestPart = jsonPart("messageCreateRequest", messageCreateRequest);
        MockMultipartFile attachmentPart = filePart(
                "attachments",
                "message-attachment-" + suffix + ".txt",
                MediaType.TEXT_PLAIN_VALUE,
                "attachment-content"
        );

        // when
        // 실제 메시지 생성 API를 multipart/form-data로 호출한다.
        MvcResult messageCreateResult = mockMvc.perform(multipart("/api/messages")
                        .file(messageCreateRequestPart)
                        .file(attachmentPart)
                        .accept(MediaType.APPLICATION_JSON))

                // then
                // 메시지 응답에는 작성자, 채널, 첨부 파일 메타데이터가 함께 포함되어야 한다.
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.content").value(messageCreateRequest.content()))
                .andExpect(jsonPath("$.channelId").value(channelId.toString()))
                .andExpect(jsonPath("$.author.id").value(authorId.toString()))
                .andExpect(jsonPath("$.attachments.length()").value(1))
                .andExpect(jsonPath("$.attachments[0].id").exists())
                .andExpect(jsonPath("$.attachments[0].fileName").value(attachmentPart.getOriginalFilename()))
                .andExpect(jsonPath("$.attachments[0].size").value(attachmentPart.getSize()))
                .andExpect(jsonPath("$.attachments[0].contentType").value(attachmentPart.getContentType()))
                .andReturn();

        JsonNode messageCreateBody = readBody(messageCreateResult);
        UUID messageId = uuidAt(messageCreateBody, "/id");
        UUID attachmentId = uuidAt(messageCreateBody, "/attachments/0/id");

        // 실제 DB에 Message, MessageFile, BinaryContent가 저장됐는지 확인한다.
        flushAndClear();
        Channel savedChannel = channelRepository.findById(channelId).orElseThrow(AssertionError::new);
        Message savedMessage = messageRepository.findById(messageId).orElseThrow(AssertionError::new);
        List<MessageFile> savedMessageFiles = messageFileRepository.findAllByMessage_Id(messageId);

        assertThat(savedChannel.getType()).isEqualTo(ChannelType.PUBLIC);
        assertThat(savedMessage.getContent()).isEqualTo(messageCreateRequest.content());
        assertThat(savedMessage.getChannelId()).isEqualTo(channelId);
        assertThat(savedMessage.getAuthor().getId()).isEqualTo(authorId);
        assertThat(savedMessageFiles).hasSize(1);
        assertThat(savedMessageFiles.get(0).getFileId()).isEqualTo(attachmentId);
        assertThat(binaryContentRepository.findById(attachmentId)).isPresent();

        // when
        // 채널별 메시지 목록 API를 호출해 방금 만든 메시지가 Querydsl 목록 조회 경로로 조회되는지 확인한다.
        mockMvc.perform(get("/api/messages")
                        .param("channelId", channelId.toString())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "createdAt,desc")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                // 목록 응답은 PageResponse 형태이며, 첨부 파일까지 함께 내려와야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(messageId.toString()))
                .andExpect(jsonPath("$.content[0].content").value(messageCreateRequest.content()))
                .andExpect(jsonPath("$.content[0].attachments[0].id").value(attachmentId.toString()))
                .andExpect(jsonPath("$.hasNext").value(false));

        // when
        // 첨부 파일 다운로드 API를 호출한다.
        mockMvc.perform(get("/api/binaryContents/{binaryContentId}/download", attachmentId))

                // then
                // BinaryContentService와 LocalBinaryContentStorage가 실제 저장 파일을 읽어 응답해야 한다.
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(header().longValue("Content-Length", attachmentPart.getSize()))
                .andExpect(content().bytes(attachmentPart.getBytes()));
    }

    @Test
    @DisplayName("비공개 채널과 읽음 상태 통합 성공 - 참여자별 ReadStatus 생성 후 수정 가능")
    void privateChannelAndReadStatus_flowCreatesAndUpdatesParticipantReadStatuses() throws Exception {
        // given
        // 비공개 채널 생성은 Channel 저장뿐 아니라 참여자별 ReadStatus bulk insert까지 수행한다.
        // 두 사용자를 실제 API로 만든 뒤 PRIVATE 채널 생성 API를 호출한다.
        String suffix = uniqueSuffix();
        UUID firstUserId = createUser("privateUserA-" + suffix, "private-a-" + suffix + "@gmail.com");
        UUID secondUserId = createUser("privateUserB-" + suffix, "private-b-" + suffix + "@gmail.com");

        PrivateChannelCreateRequest request = new PrivateChannelCreateRequest(
                List.of(firstUserId, secondUserId)
        );

        // when
        // POST /api/channels/private 요청을 전송한다.
        MvcResult createResult = mockMvc.perform(post("/api/channels/private")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))

                // then
                // 응답은 PRIVATE 채널과 참여자 목록을 포함해야 한다.
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value(ChannelType.PRIVATE.name()))
                .andExpect(jsonPath("$.participants.length()").value(2))
                .andReturn();

        UUID channelId = uuidAt(readBody(createResult), "/id");

        // 실제 DB에서 채널과 참여자별 ReadStatus가 생성됐는지 확인한다.
        flushAndClear();
        Channel savedChannel = channelRepository.findById(channelId).orElseThrow(AssertionError::new);
        List<ReadStatus> channelReadStatuses = readStatusRepository.findByChannelId(channelId);

        assertThat(savedChannel.getType()).isEqualTo(ChannelType.PRIVATE);
        assertThat(channelReadStatuses)
                .extracting(ReadStatus::getUserId)
                .containsExactlyInAnyOrder(firstUserId, secondUserId);

        // when
        // 특정 사용자의 읽음 상태 목록을 조회한다.
        MvcResult readStatusListResult = mockMvc.perform(get("/api/readStatuses")
                        .param("userId", firstUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                // 첫 번째 사용자에게 PRIVATE 채널의 ReadStatus가 조회되어야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(firstUserId.toString()))
                .andExpect(jsonPath("$[0].channelId").value(channelId.toString()))
                .andReturn();

        UUID readStatusId = uuidAt(readBody(readStatusListResult), "/0/id");

        // when
        // 읽음 상태의 lastReadAt을 실제 API로 수정한다.
        Instant newLastReadAt = Instant.parse("2026-07-28T02:30:30Z");
        ReadStatusUpdateRequest updateRequest = new ReadStatusUpdateRequest(newLastReadAt);
        mockMvc.perform(patch("/api/readStatuses/{readStatusId}", readStatusId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))

                // then
                // 수정 API 응답은 같은 ReadStatus id와 channelId를 유지해야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(readStatusId.toString()))
                .andExpect(jsonPath("$.userId").value(firstUserId.toString()))
                .andExpect(jsonPath("$.channelId").value(channelId.toString()));

        // DB에서도 lastReadAt이 요청값으로 갱신됐는지 확인한다.
        flushAndClear();
        ReadStatus updatedReadStatus = readStatusRepository.findById(readStatusId).orElseThrow(AssertionError::new);
        assertThat(updatedReadStatus.getLastReadAt()).isEqualTo(newLastReadAt);

        // when
        // 사용자별 채널 목록 API를 호출한다.
        mockMvc.perform(get("/api/channels")
                        .param("userId", firstUserId.toString())
                        .accept(MediaType.APPLICATION_JSON))

                // then
                // PRIVATE 채널은 참여자인 사용자에게 visible channel로 조회되어야 한다.
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(channelId.toString()))
                .andExpect(jsonPath("$[0].type").value(ChannelType.PRIVATE.name()));
    }

    @Test
    @DisplayName("사용자 목록, 수정, 삭제 통합 성공 - 실제 DB에 변경사항 반영")
    void userListUpdateAndDelete_flowPersistsUpdatesAndRemovesUser() throws Exception {
        // given
        // 사용자 관련 주요 API 중 기존 생성 흐름에서 다루지 않은 목록 조회, 수정, 삭제를 한 흐름에서 검증한다.
        // 모든 호출은 실제 Controller, Service, Repository, DB를 거친다.
        String suffix = uniqueSuffix();
        UUID firstUserId = createUser("userListA-" + suffix, "user-list-a-" + suffix + "@gmail.com");
        UUID secondUserId = createUser("userListB-" + suffix, "user-list-b-" + suffix + "@gmail.com");

        // when
        // 사용자 목록 API를 호출한다.
        MvcResult listResult = mockMvc.perform(get("/api/users")
                        .accept(MediaType.APPLICATION_JSON))

                // then
                // 방금 생성한 두 사용자가 실제 목록 응답에 포함되어야 한다.
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andReturn();

        JsonNode listBody = readBody(listResult);
        assertThat(listBody.findValuesAsText("id"))
                .contains(firstUserId.toString(), secondUserId.toString());

        // when
        // 첫 번째 사용자의 username, password, email, profile을 multipart PATCH로 수정한다.
        UserUpdateRequest updateRequest = new UserUpdateRequest(
                "updatedUser-" + suffix,
                "updatedPassword",
                "updated-user-" + suffix + "@gmail.com"
        );
        MockMultipartFile updateRequestPart = jsonPart("userUpdateRequest", updateRequest);
        MockMultipartFile profilePart = filePart(
                "profile",
                "updated-profile-" + suffix + ".png",
                MediaType.IMAGE_PNG_VALUE,
                "updated-profile"
        );

        MvcResult updateResult = mockMvc.perform(multipart("/api/users/{userId}", firstUserId)
                        .file(updateRequestPart)
                        .file(profilePart)
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        })
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(firstUserId.toString()))
                .andExpect(jsonPath("$.username").value(updateRequest.newUsername()))
                .andExpect(jsonPath("$.email").value(updateRequest.newEmail()))
                .andExpect(jsonPath("$.profile.id").exists())
                .andExpect(jsonPath("$.profile.fileName").value(profilePart.getOriginalFilename()))
                .andReturn();

        UUID updatedProfileId = uuidAt(readBody(updateResult), "/profile/id");

        flushAndClear();
        User updatedUser = userRepository.findById(firstUserId).orElseThrow(AssertionError::new);
        assertThat(updatedUser.getUsername()).isEqualTo(updateRequest.newUsername());
        assertThat(updatedUser.getPassword()).isEqualTo(updateRequest.newPassword());
        assertThat(updatedUser.getEmail()).isEqualTo(updateRequest.newEmail());
        assertThat(updatedUser.getProfileId()).isEqualTo(updatedProfileId);
        assertThat(binaryContentRepository.findById(updatedProfileId)).isPresent();

        // when
        // 수정한 사용자를 삭제한다.
        mockMvc.perform(delete("/api/users/{userId}", firstUserId))
                .andExpect(status().isNoContent());

        // then
        // User와 UserStatus가 실제 DB에서 제거되고, 대조군 사용자는 남아 있어야 한다.
        flushAndClear();
        assertThat(userRepository.findById(firstUserId)).isEmpty();
        assertThat(userStatusRepository.findByUserId(firstUserId)).isEmpty();
        assertThat(userRepository.findById(secondUserId)).isPresent();
    }

    @Test
    @DisplayName("채널 수정, 삭제 통합 성공 - 실제 DB에 변경사항 반영")
    void channelUpdateAndDelete_flowPersistsUpdatesAndRemovesChannel() throws Exception {
        // given
        // 채널 생성은 기존 통합 테스트에서 검증하므로, 여기서는 PUBLIC 채널을 만든 뒤 수정과 삭제 흐름을 검증한다.
        String suffix = uniqueSuffix();
        UUID channelId = createPublicChannel(
                "channel-update-" + suffix,
                "channel update description"
        );

        // when
        // PUBLIC 채널은 수정 가능하므로 이름과 설명을 변경한다.
        ChannelUpdateRequest updateRequest = new ChannelUpdateRequest(
                "updated-channel-" + suffix,
                "updated channel description"
        );
        mockMvc.perform(patch("/api/channels/{channelId}", channelId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(channelId.toString()))
                .andExpect(jsonPath("$.type").value(ChannelType.PUBLIC.name()))
                .andExpect(jsonPath("$.name").value(updateRequest.newName()))
                .andExpect(jsonPath("$.description").value(updateRequest.newDescription()));

        flushAndClear();
        Channel updatedChannel = channelRepository.findById(channelId).orElseThrow(AssertionError::new);
        assertThat(updatedChannel.getName()).isEqualTo(updateRequest.newName());
        assertThat(updatedChannel.getDescription()).isEqualTo(updateRequest.newDescription());

        // when
        // 수정한 채널을 삭제한다.
        mockMvc.perform(delete("/api/channels/{channelId}", channelId))
                .andExpect(status().isNoContent());

        // then
        // Channel row가 실제 DB에서 제거되어야 한다.
        flushAndClear();
        assertThat(channelRepository.findById(channelId)).isEmpty();
    }

    @Test
    @DisplayName("메시지 수정, 삭제 통합 성공 - 실제 DB에 변경사항 반영")
    void messageUpdateAndDelete_flowPersistsUpdatesAndRemovesMessage() throws Exception {
        // given
        // 메시지 생성과 목록 조회는 기존 통합 테스트에서 검증한다.
        // 여기서는 생성된 메시지를 수정하고 삭제하는 API 흐름을 실제 DB와 함께 확인한다.
        String suffix = uniqueSuffix();
        UUID authorId = createUser("messageUpdateAuthor-" + suffix, "message-update-author-" + suffix + "@gmail.com");
        UUID channelId = createPublicChannel(
                "message-update-channel-" + suffix,
                "message update channel description"
        );
        UUID messageId = createMessage("message before update", channelId, authorId);

        // when
        // 메시지 내용을 수정한다.
        MessageUpdateRequest updateRequest = new MessageUpdateRequest("message after update");
        mockMvc.perform(patch("/api/messages/{messageId}", messageId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(messageId.toString()))
                .andExpect(jsonPath("$.content").value(updateRequest.newContent()))
                .andExpect(jsonPath("$.channelId").value(channelId.toString()))
                .andExpect(jsonPath("$.author.id").value(authorId.toString()));

        flushAndClear();
        Message updatedMessage = messageRepository.findById(messageId).orElseThrow(AssertionError::new);
        assertThat(updatedMessage.getContent()).isEqualTo(updateRequest.newContent());

        // when
        // 수정한 메시지를 삭제한다.
        mockMvc.perform(delete("/api/messages/{messageId}", messageId))
                .andExpect(status().isNoContent());

        // then
        // Message row가 실제 DB에서 제거되고, 메시지 파일 연결도 남아 있지 않아야 한다.
        flushAndClear();
        assertThat(messageRepository.findById(messageId)).isEmpty();
        assertThat(messageFileRepository.findAllByMessage_Id(messageId)).isEmpty();
    }

    private UUID createUser(String username, String email) throws Exception {
        UserCreateRequest request = new UserCreateRequest(
                username,
                "integrationPassword",
                email
        );

        MvcResult result = mockMvc.perform(multipart("/api/users")
                        .file(jsonPart("userCreateRequest", request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        return uuidAt(readBody(result), "/id");
    }

    private UUID createPublicChannel(String name, String description) throws Exception {
        PublicChannelCreateRequest request = new PublicChannelCreateRequest(name, description);

        MvcResult result = mockMvc.perform(post("/api/channels/public")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.type").value(ChannelType.PUBLIC.name()))
                .andReturn();

        return uuidAt(readBody(result), "/id");
    }

    private UUID createMessage(String content, UUID channelId, UUID authorId) throws Exception {
        MessageCreateRequest request = new MessageCreateRequest(content, channelId, authorId);

        MvcResult result = mockMvc.perform(multipart("/api/messages")
                        .file(jsonPart("messageCreateRequest", request))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        return uuidAt(readBody(result), "/id");
    }

    private MockMultipartFile jsonPart(String name, Object value) throws Exception {
        return new MockMultipartFile(
                name,
                "",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(value)
        );
    }

    private MockMultipartFile filePart(String name, String fileName, String contentType, String content) {
        return new MockMultipartFile(
                name,
                fileName,
                contentType,
                content.getBytes(StandardCharsets.UTF_8)
        );
    }

    private JsonNode readBody(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private UUID uuidAt(JsonNode node, String pointer) {
        return UUID.fromString(node.at(pointer).asText());
    }

    private String uniqueSuffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }
}
