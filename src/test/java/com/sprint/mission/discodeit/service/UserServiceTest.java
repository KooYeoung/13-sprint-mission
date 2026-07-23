package com.sprint.mission.discodeit.service;

import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.dto.command.user.UserUpdateCommand;
import com.sprint.mission.discodeit.dto.command.userStatus.UserStatusCreateCommand;
import com.sprint.mission.discodeit.dto.response.BinaryContentDto;
import com.sprint.mission.discodeit.dto.response.UserDto;
import com.sprint.mission.discodeit.dto.response.UserStatusDto;
import com.sprint.mission.discodeit.entity.BinaryContent;
import com.sprint.mission.discodeit.entity.User;
import com.sprint.mission.discodeit.exception.user.UserEmailDuplicatedException;
import com.sprint.mission.discodeit.exception.user.UserNotFoundException;
import com.sprint.mission.discodeit.exception.user.UserUsernameDuplicatedException;
import com.sprint.mission.discodeit.mapper.UserMapper;
import com.sprint.mission.discodeit.repository.UserRepository;
import com.sprint.mission.discodeit.service.basic.BasicUserService;
import com.sprint.mission.discodeit.service.basic.BinaryContentService;
import com.sprint.mission.discodeit.service.basic.ReadStatusService;
import com.sprint.mission.discodeit.service.basic.UserStatusService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    BasicUserService userService;

    @Mock
    UserRepository userRepository;

    @Mock
    BinaryContentService binaryContentService;

    @Mock
    UserStatusService userStatusService;

    @Mock
    ReadStatusService readStatusService;

    @Mock
    MessageService messageService;

    @Mock
    UserMapper userMapper;

    @Nested
    @DisplayName("사용자 생성")
    class CreateUserTest {
        @Test
        @DisplayName("사용자 생성 성공")
        void create_returnsUserDto_whenValidCommand() {
            // given
            // 테스트에서 사용할 사용자 생성 요청 객체를 준비한다.
            // 실제 사용자가 회원가입할 때 넘기는 username, password, email 역할이다.
            UserCreateCommand command = createTestUserCommand();

            // 테스트에서 사용할 userId를 하나 고정해둔다.
            // UUID.randomUUID()를 여러 번 직접 쓰면 어떤 값이 어떤 객체의 id인지 헷갈릴 수 있으므로
            // 변수로 빼두면 테스트 의도가 더 명확해진다.
            UUID userId = UUID.randomUUID();

            // BasicUserService.create() 내부에서는 사용자를 저장한 뒤
            // userStatusService.create(...)를 호출하고,
            // 그 결과인 userStatusDto.isOnline() 값을 사용한다.
            //
            // 따라서 userStatusService.create(...)가 null을 반환하면
            // userStatusDto.isOnline() 호출 시 NullPointerException이 발생한다.
            // 그래서 mock 객체가 반환할 UserStatusDto를 미리 만들어둔다.
            UserStatusDto userStatusDto = createUserStatusDto(userId);

            // 서비스의 최종 반환값으로 기대하는 UserDto를 준비한다.
            // 실제 서비스는 UserDto를 직접 만들지 않고 userMapper.toDto(...)를 통해 만든다.
            // 그래서 아래 expectedDto는 "mapper가 반환해줄 결과"로 사용할 값이다.
            UserDto expectedDto = createExpectedDto(userId, command, userStatusDto.isOnline());

            // userRepository.save(...)는 mock이므로 실제 DB 저장을 하지 않는다.
            // Mockito mock은 별도 설정이 없으면 객체를 저장하지 않고 기본값(null)을 반환한다.
            //
            // 서비스 로직에서는 save(...)가 저장된 User를 반환한다고 기대하므로,
            // 테스트에서는 "save로 들어온 User 객체를 그대로 반환하라"고 설정한다.
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            // BasicUserService.create()는 사용자 생성 중 프로필 파일 저장을 시도한다.
            // 이 테스트는 프로필 파일 없는 사용자 생성 성공 케이스이므로 Optional.empty()를 반환하게 한다.
            given(binaryContentService.create(null))
                    .willReturn(Optional.empty());

            // BasicUserService.create() 내부에서 userStatusService.create(...)를 호출한다.
            // 이 반환값의 isOnline()을 바로 사용하므로 반드시 UserStatusDto를 반환하도록 설정해야 한다.
            given(userStatusService.create(any(User.class), any(UserStatusCreateCommand.class)))
                    .willReturn(userStatusDto);

            // BasicUserService.create()의 마지막 줄은 userMapper.toDto(...)이다.
            // mapper도 mock이므로 실제 매핑을 하지 않는다.
            // 따라서 어떤 DTO를 반환해야 하는지 테스트에서 직접 지정한다.
            given(userMapper.toDto(any(User.class), eq(userStatusDto.isOnline())))
                    .willReturn(expectedDto);

            // when
            // 실제 테스트 대상 메서드를 호출한다.
            // 여기서부터 BasicUserService.create()의 흐름이 실행된다.
            UserDto result = userService.create(command, null);

            // then
            // 서비스가 최종적으로 반환한 DTO가 우리가 기대한 DTO와 같은지 검증한다.
            assertThat(result).isEqualTo(expectedDto);

            // save(...)에 실제로 어떤 User 객체가 전달됐는지 확인하기 위해 ArgumentCaptor를 사용한다.
            // 반환값만 검증하면 "정말 command 값으로 User를 만들었는지"는 확인하기 어렵다.
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            // captor에 저장된 값은 userRepository.save(...) 호출 당시 전달된 User 객체다.
            User user = captor.getValue();

            // command에 들어 있던 username, email이 User 엔티티에 제대로 반영됐는지 검증한다.
            assertThat(user.getUsername()).isEqualTo(command.username());
            assertThat(user.getEmail()).isEqualTo(command.email());
        }

        @Test
        @DisplayName("사용자 생성 실패 - 이메일 중복")
        void create_throwsUserEmailDuplicatedException_whenEmailAlreadyExists() {
            // given
            // 테스트에서 사용할 사용자 생성 요청 객체를 준비한다.
            UserCreateCommand command = createTestUserCommand();

            // 이미 같은 이메일을 가진 사용자가 존재하는 상황을 만든다.
            // BasicUserService.create()는 사용자 생성 전에 existsByEmail()로 이메일 중복을 검사한다.
            given(userRepository.existsByEmail(command.email())).willReturn(true);

            // when & then
            // 중복된 이메일로 사용자를 생성하려고 하면 UserEmailDuplicatedException이 발생해야 한다.
            assertThatThrownBy(() -> userService.create(command, null))
                    .isInstanceOf(UserEmailDuplicatedException.class);

            // 이메일 중복 검증까지만 실행됐는지 확인한다.
            verify(userRepository).existsByEmail(command.email());
            verify(userRepository, never()).existsByUsername(anyString());

            // 이메일 중복 검증에서 예외가 발생했으므로 이후 생성 절차는 실행되면 안 된다.
            verify(binaryContentService, never()).create(any());
            verify(userRepository, never()).save(any(User.class));
            verify(userStatusService, never()).create(any(User.class), any(UserStatusCreateCommand.class));
            verify(userMapper, never()).toDto(any(User.class), anyBoolean());

        }

        @Test
        @DisplayName("사용자 생성 실패 - 사용자명 중복")
        void create_throwsUserUsernameDuplicatedException_whenUsernameAlreadyExists() {
            // given
            // 테스트에서 사용할 사용자 생성 요청 객체를 준비한다.
            UserCreateCommand command = createTestUserCommand();

            // BasicUserService.create()는 이메일 중복을 먼저 검사한 뒤 사용자명 중복을 검사한다.
            // 이 테스트는 "사용자명 중복" 케이스이므로 이메일은 중복되지 않은 상황으로 둔다.
            given(userRepository.existsByEmail(command.email())).willReturn(false);

            // 이미 같은 사용자명을 가진 사용자가 존재하는 상황을 만든다.
            given(userRepository.existsByUsername(command.username())).willReturn(true);

            // when & then
            // 중복된 사용자명으로 사용자를 생성하려고 하면 UserUsernameDuplicatedException 발생해야 한다.
            assertThatThrownBy(() -> userService.create(command, null))
                    .isInstanceOf(UserUsernameDuplicatedException.class);

            // 이메일 중복 검증 이후 사용자명 중복 검증까지 실행됐는지 확인한다.
            verify(userRepository).existsByEmail(command.email());
            verify(userRepository).existsByUsername(command.username());

            // 사용자명 중복 검증에서 예외가 발생했으므로 이후 생성 절차는 실행되면 안 된다.
            verify(binaryContentService, never()).create(any());
            verify(userRepository, never()).save(any(User.class));
            verify(userStatusService, never()).create(any(User.class), any(UserStatusCreateCommand.class));
            verify(userMapper, never()).toDto(any(User.class), anyBoolean());
        }

        @Test
        @DisplayName("사용자 생성 성공 - 프로필 이미지가 있으면 함께 저장")
        void create_returnsUserDtoWithProfile_whenProfileFileExists() {
            // given
            // create()는 사용자 생성 시 프로필 파일 저장을 먼저 시도한다.
            // 이 테스트는 프로필 이미지가 전달되면 저장된 User에 해당 프로필이 연결되는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();

            // binaryContentService.create(file)가 반환할 프로필 정보를 준비한다.
            // 이 값이 User 생성자에 전달되어 저장되는지 ArgumentCaptor로 검증한다.
            BinaryContent profile = new BinaryContent("filename", "contentType", 1000L);

            MockMultipartFile file = new MockMultipartFile(
                    "profile",
                    "profile.png",
                    "image/png",
                    "profile".getBytes()
            );

            UserStatusDto userStatusDto = createUserStatusDto(userId);
            UserDto expectedDto = createExpectedDto(userId, command, userStatusDto.isOnline());

            // 이메일과 사용자명이 중복되지 않아 사용자 생성이 가능한 상황을 만든다.
            given(userRepository.existsByEmail(command.email())).willReturn(false);
            given(userRepository.existsByUsername(command.username())).willReturn(false);

            // 프로필 파일 저장 결과로 BinaryContent가 반환되는 상황을 만든다.
            given(binaryContentService.create(file)).willReturn(Optional.of(profile));

            // mock repository는 기본적으로 save() 호출 시 null을 반환한다.
            // 서비스는 저장 결과를 userStatusService와 mapper에 넘기므로 저장된 User를 그대로 반환하게 한다.
            given(userRepository.save(any(User.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            given(userStatusService.create(any(User.class), any(UserStatusCreateCommand.class)))
                    .willReturn(userStatusDto);

            given(userMapper.toDto(any(User.class), eq(userStatusDto.isOnline())))
                    .willReturn(expectedDto);

            // when
            UserDto userDto = userService.create(command, file);

            // then
            assertThat(userDto).isEqualTo(expectedDto);

            // 실제 저장된 User를 캡처해서 command 값과 profile이 반영됐는지 확인한다.
            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());

            User user = captor.getValue();
            assertThat(user.getUsername()).isEqualTo(command.username());
            assertThat(user.getEmail()).isEqualTo(command.email());
            assertThat(user.getProfile()).isEqualTo(profile);

            verify(userRepository).existsByEmail(command.email());
            verify(userRepository).existsByUsername(command.username());
            verify(binaryContentService).create(file);
            verify(userStatusService).create(eq(user), any(UserStatusCreateCommand.class));
            verify(userMapper).toDto(user, userStatusDto.isOnline());
        }

    }

    @Nested
    @DisplayName("사용자 조회")
    class FindUserTest {

        @Test
        @DisplayName("사용자 단건 조회 성공")
        void findById_returnsUserDto_whenUserExists() {
            // given
            // 조회할 사용자 id와 repository가 반환할 User 엔티티를 준비한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // findById()는 userMapper.toDto(user)를 통해 UserDto를 반환하므로
            // mapper가 반환할 기대 DTO를 준비한다.
            UserDto expectedDto = createExpectedDto(userId, command, false);

            // userId로 조회했을 때 준비한 user가 반환되도록 설정한다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // user를 DTO로 변환했을 때 준비한 expectedDto가 반환되도록 설정한다.
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            // 서비스의 사용자 단건 조회 로직을 실행한다.
            UserDto result = userService.findById(userId);

            //then
            // 서비스의 반환값이 기대한 DTO와 같은지 검증한다.
            assertThat(result).isEqualTo(expectedDto);

            // repository가 userId로 조회되었는지 검증한다.
            verify(userRepository).findById(userId);

            // mapper가 조회된 user를 DTO로 변환했는지 검증한다.
            verify(userMapper).toDto(user);
        }

        @Test
        @DisplayName("사용자 단건 조회 실패 - 존재하지 않는 사용자")
        void findById_throwsUserNotFoundException_whenUserDoesNotExist() {
            // given
            // 조회할 사용자 id를 준비한다.
            UUID userId = UUID.randomUUID();

            // userId로 조회했을 때 사용자가 존재하지 않는 상황을 설정한다.
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            // 존재하지 않는 사용자를 조회하면 UserNotFoundException이 발생해야 한다.
            assertThatThrownBy(() -> userService.findById(userId))
                    .isInstanceOf(UserNotFoundException.class);

            // 예외가 발생했으므로 mapper는 호출되지 않아야 한다.
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("사용자 목록 조회 성공 - 사용자 목록 반환")
        void findAll_returnsUserDtoList_whenUsersExist() {
            // given
            // 조회 결과로 사용할 사용자 생성 요청 목록을 준비한다.
            List<UserCreateCommand> commands = List.of(createTestUserCommand(), createTestUserCommand());

            // 생성 요청을 기반으로 repository가 반환할 User 엔티티 목록을 준비한다.
            List<User> users = commands.stream()
                    .map(c -> new User(c, null))
                    .toList();

            // mapper가 반환할 UserDto 목록을 준비한다.
            List<UserDto> userDtos = commands.stream()
                    .map(UserServiceTest.this::createExpectedDto)
                    .toList();

            // repository.findAll() 호출 시 준비한 users가 반환되도록 설정한다.
            given(userRepository.findAll()).willReturn(users);

            // findAll()은 조회된 User 목록을 하나씩 UserDto로 변환한다.
            // 각 User가 어떤 UserDto로 변환될지 명확하게 지정한다.
            given(userMapper.toDto(users.get(0))).willReturn(userDtos.get(0));
            given(userMapper.toDto(users.get(1))).willReturn(userDtos.get(1));

            // when
            // 사용자 목록 조회 서비스 로직을 실행한다.
            List<UserDto> results = userService.findAll();

            // then
            // 서비스 결과가 준비한 DTO 목록과 같은지 검증한다.
            assertThat(results).isEqualTo(userDtos);

            // repository.findAll()이 호출되었는지 검증한다.
            verify(userRepository).findAll();

            // 조회된 각 User가 DTO로 변환되었는지 검증한다.
            verify(userMapper).toDto(users.get(0));
            verify(userMapper).toDto(users.get(1));

            /*
             * 아래처럼 mapper가 호출될 때마다 순서대로 다른 값을 반환하도록 설정할 수도 있다.
             *
             * given(userMapper.toDto(any(User.class)))
             *         .willReturn(userDtos.get(0), userDtos.get(1));
             *
             * verify(userMapper, times(users.size())).toDto(any(User.class));
             */
        }

        @Test
        @DisplayName("사용자 목록 조회 성공 - 사용자가 없으면 빈 목록 반환")
        void findAll_returnsEmptyList_whenUsersDoNotExist() {
            // given
            // repository.findAll() 호출 시 조회된 사용자가 없는 상황을 설정한다.
            given(userRepository.findAll()).willReturn(List.of());

            // when
            // 사용자 목록 조회 서비스 로직을 실행한다.
            List<UserDto> results = userService.findAll();

            // then
            // 조회된 사용자가 없으므로 서비스 결과는 빈 목록이어야 한다.
            assertThat(results).isEmpty();

            // repository.findAll()이 호출되었는지 검증한다.
            verify(userRepository).findAll();

            // 변환할 User가 없으므로 mapper는 호출되지 않아야 한다.
            verify(userMapper, never()).toDto(any(User.class));
        }
    }

    @Nested
    @DisplayName("사용자 수정")
    class UpdateUserTest {
        @Test
        @DisplayName("사용자 수정 성공")
        void update_returnsUpdatedUserDto_whenValidCommand() {
            // given
            // update()는 먼저 userId로 기존 사용자를 조회한다.
            // 따라서 repository가 반환할 기존 User 엔티티를 준비한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 기존 사용자와 다른 값으로 수정 요청을 만들어
            // 중복 검사와 엔티티 수정 로직이 모두 실행되도록 한다.
            UserUpdateCommand updateCommand = createUpdateCommand();

            // 서비스는 수정된 User를 직접 DTO로 만들지 않고 mapper에 위임한다.
            // 따라서 mapper가 반환할 DTO를 미리 정해 최종 반환값을 검증한다.
            UserDto expectedDto = createExpectedDto(userId, updateCommand, false);

            // userId로 조회하면 준비한 User 엔티티가 존재하는 상황을 만든다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // 변경할 이메일이 기존 사용자와 다르므로 중복 검사가 실행된다.
            // 성공 케이스이므로 같은 이메일은 존재하지 않는다고 설정한다.
            given(userRepository.existsByEmail(updateCommand.email())).willReturn(false);

            // 변경할 사용자명도 기존 사용자와 다르므로 중복 검사가 실행된다.
            // 성공 케이스이므로 같은 사용자명은 존재하지 않는다고 설정한다.
            given(userRepository.existsByUsername(updateCommand.username())).willReturn(false);

            // 이번 테스트는 프로필 이미지 변경이 없는 사용자 정보 수정만 검증한다.
            // 따라서 파일 생성 결과는 Optional.empty()로 설정한다.
            given(binaryContentService.create(null)).willReturn(Optional.empty());

            // update()는 수정된 User를 저장한 뒤 저장 결과를 mapper에 넘긴다.
            // mock repository는 기본적으로 null을 반환하므로 명시적으로 user를 반환하게 한다.
            given(userRepository.save(user)).willReturn(user);

            // 최종 반환값 검증을 위해 mapper가 expectedDto를 반환하도록 설정한다.
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            UserDto updateUserDto = userService.update(userId, updateCommand, null);

            // then
            assertThat(updateUserDto).isEqualTo(expectedDto);

            // 반환 DTO뿐 아니라 실제 User 엔티티 상태도 수정됐는지 확인한다.
            assertThat(user.getUsername()).isEqualTo(updateCommand.username());
            assertThat(user.getPassword()).isEqualTo(updateCommand.password());
            assertThat(user.getEmail()).isEqualTo(updateCommand.email());

            // update()가 기대한 협력 객체들을 호출했는지 확인한다.
            verify(userRepository).findById(userId);
            verify(userRepository).existsByEmail(updateCommand.email());
            verify(userRepository).existsByUsername(updateCommand.username());
            verify(binaryContentService).create(null);
            verify(userRepository).save(user);
            verify(userMapper).toDto(user);

        }

        @Test
        @DisplayName("사용자 수정 실패 - 존재하지 않는 사용자")
        void update_throwsUserNotFoundException_whenUserDoesNotExist() {
            // given
            // update()는 먼저 userId로 수정할 사용자를 조회한다.
            // 존재하지 않는 사용자 수정 요청을 검증하기 위해 임의의 userId를 준비한다.
            UUID userId = UUID.randomUUID();

            // 사용자가 존재하지 않아도 update() 호출에는 수정 요청 객체가 필요하다.
            // 이 테스트에서는 조회 실패가 목적이므로 command의 값 자체는 중요하지 않다.
            UserUpdateCommand updateCommand = createUpdateCommand();

            // userId로 조회했을 때 사용자가 없는 상황을 만든다.
            // Optional.empty()가 반환되면 서비스는 UserNotFoundException을 던져야 한다.
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            // 존재하지 않는 사용자를 수정하려고 하면 UserNotFoundException이 발생해야 한다.
            assertThatThrownBy(() -> userService.update(userId, updateCommand, null))
                    .isInstanceOf(UserNotFoundException.class);

            // 조회 단계에서 예외가 발생했으므로 이후 수정 절차는 실행되면 안 된다.
            verify(userRepository).findById(userId);
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).existsByUsername(anyString());
            verify(binaryContentService, never()).create(any());
            verify(userRepository, never()).save(any(User.class));
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("사용자 수정 실패 - 이메일 중복")
        void update_throwsUserEmailDuplicatedException_whenEmailAlreadyExists() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 조회 성공 이후 이메일 중복 검증에서 실패하는 상황을 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 기존 사용자와 다른 이메일로 수정 요청을 준비한다.
            // 그래야 서비스가 이메일 중복 검사를 실행한다.
            UserUpdateCommand updateCommand = createUpdateCommand();

            // userId로 조회하면 수정 대상 사용자가 존재하는 상황을 만든다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // 변경하려는 이메일이 이미 다른 사용자에게 사용 중인 상황을 만든다.
            // 이 값이 true이면 서비스는 UserEmailDuplicatedException을 던져야 한다.
            given(userRepository.existsByEmail(updateCommand.email())).willReturn(true);

            // when & then
            // 이메일이 중복된 사용자 수정 요청은 실패해야 한다.
            assertThatThrownBy(() -> userService.update(userId, updateCommand, null))
                    .isInstanceOf(UserEmailDuplicatedException.class);

            // 수정 대상 조회와 이메일 중복 검사까지 실행됐는지 확인한다.
            verify(userRepository).findById(userId);
            verify(userRepository).existsByEmail(updateCommand.email());

            // 이메일 중복 단계에서 예외가 발생했으므로 이후 로직은 실행되면 안 된다.
            verify(userRepository, never()).existsByUsername(anyString());
            verify(binaryContentService, never()).create(any());
            verify(userRepository, never()).save(any(User.class));
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("사용자 수정 실패 - 사용자명 중복")
        void update_throwsUserUsernameDuplicatedException_whenUsernameAlreadyExists() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 사용자는 존재하지만, 변경하려는 사용자명이 이미 사용 중인 경우를 검증한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 이메일은 기존 이메일 그대로 사용한다.
            // 그래야 이메일 중복 검사는 건너뛰고, 사용자명 중복 검사만 검증할 수 있다.
            UserUpdateCommand updateCommand = new UserUpdateCommand(
                    "duplicatedUsername",
                    "changePassword",
                    command.email()
            );

            // userId로 조회하면 수정 대상 사용자가 존재하는 상황을 만든다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // 변경하려는 사용자명이 이미 다른 사용자에게 사용 중인 상황을 만든다.
            // 이 값이 true이면 서비스는 UserUsernameDuplicatedException을 던져야 한다.
            given(userRepository.existsByUsername(updateCommand.username())).willReturn(true);

            // when & then
            // 사용자명이 중복된 사용자 수정 요청은 실패해야 한다.
            assertThatThrownBy(() -> userService.update(userId, updateCommand, null))
                    .isInstanceOf(UserUsernameDuplicatedException.class);

            // 수정 대상 조회와 사용자명 중복 검사까지 실행됐는지 확인한다.
            verify(userRepository).findById(userId);
            verify(userRepository).existsByUsername(updateCommand.username());

            // 이메일은 기존 값과 같으므로 이메일 중복 검사는 실행되지 않아야 한다.
            verify(userRepository, never()).existsByEmail(anyString());

            // 사용자명 중복 단계에서 예외가 발생했으므로 이후 수정 절차는 실행되면 안 된다.
            verify(binaryContentService, never()).create(any());
            verify(userRepository, never()).save(any(User.class));
            verify(userMapper, never()).toDto(any(User.class));
        }

        @Test
        @DisplayName("사용자 수정 성공 - 이메일과 사용자명이 같으면 중복 검사를 건너뜀")
        void update_skipsDuplicateChecks_whenUsernameAndEmailAreUnchanged() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 사용자명과 이메일이 기존 값과 같을 때 중복 검사를 건너뛰는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 사용자명과 이메일은 기존 값 그대로 사용하고, 비밀번호만 변경한다.
            // 서비스는 사용자명과 이메일이 모두 기존 값과 같으면 중복 검사 없이 수정을 진행한다.
            UserUpdateCommand updateCommand = new UserUpdateCommand(
                    command.username(),
                    "changePassword",
                    command.email()
            );

            // 서비스는 수정된 User를 mapper에 넘겨 DTO로 변환한다.
            // mapper가 반환할 최종 DTO를 미리 준비한다.
            UserDto expectedDto = createExpectedDto(userId, updateCommand, false);

            // userId로 조회하면 수정 대상 사용자가 존재하는 상황을 만든다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // 이 테스트는 프로필 이미지 변경 없이 사용자 정보만 수정하는 케이스다.
            given(binaryContentService.create(null)).willReturn(Optional.empty());

            // mock repository는 기본적으로 save() 호출 시 null을 반환한다.
            // 서비스는 save() 결과를 mapper에 넘기므로 저장된 user를 반환하게 설정한다.
            given(userRepository.save(user)).willReturn(user);

            // 최종 반환값 검증을 위해 mapper가 expectedDto를 반환하도록 설정한다.
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            UserDto resultDto = userService.update(userId, updateCommand, null);

            // then
            // 서비스 반환값이 mapper가 반환한 DTO와 같은지 확인한다.
            assertThat(resultDto).isEqualTo(expectedDto);

            // 비밀번호는 변경 요청 값으로 수정되어야 한다.
            assertThat(user.getPassword()).isEqualTo(updateCommand.password());

            // 수정 대상 사용자를 조회했는지 확인한다.
            verify(userRepository).findById(userId);

            // 사용자명과 이메일이 기존 값과 같으므로 중복 검사는 실행되지 않아야 한다.
            verify(userRepository, never()).existsByEmail(anyString());
            verify(userRepository, never()).existsByUsername(anyString());

            // 프로필 이미지 변경은 없지만, 서비스는 파일 생성 시도를 하고 Optional.empty()를 받는다.
            verify(binaryContentService).create(null);

            // 중복 검사 없이 사용자 수정과 저장은 정상적으로 진행되어야 한다.
            verify(userRepository).save(user);

            // 기존 프로필 이미지가 없으므로 파일 삭제는 실행되지 않아야 한다.
            verify(binaryContentService, never()).delete(any(BinaryContent.class));

            // 저장된 User가 DTO로 변환되어야 한다.
            verify(userMapper).toDto(user);

        }

        @Test
        @DisplayName("사용자 수정 성공 - 새 프로필 이미지가 있으면 기존 이미지를 교체")
        void update_replacesProfileImage_whenNewProfileFileExists() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 기존 프로필 이미지가 있는 사용자가 새 프로필 이미지로 수정될 때,
            // 새 이미지로 교체되고 기존 이미지가 삭제되는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();

            // 기존 프로필과 새 프로필의 id가 달라야 기존 프로필 삭제 분기가 실행된다.
            UUID oldProfileId = UUID.randomUUID();
            BinaryContent oldProfile = new BinaryContent("file", "contentType", 1000L);
            ReflectionTestUtils.setField(oldProfile, "id", oldProfileId);

            User user = new User(command, oldProfile);

            MockMultipartFile newFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
            UUID newProfileId = UUID.randomUUID();
            BinaryContent newProfileContent = new BinaryContent(newFile.getOriginalFilename(), newFile.getContentType(), newFile.getSize());
            ReflectionTestUtils.setField(newProfileContent, "id", newProfileId);

            UserUpdateCommand updateCommand = createUpdateCommand();

            BinaryContentDto newProfile = new BinaryContentDto(newProfileId, newFile.getOriginalFilename(), newFile.getSize(), newFile.getContentType());
            UserDto expectedDto = createExpectedDto(userId, updateCommand, false, newProfile);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsByEmail(updateCommand.email())).willReturn(false);
            given(userRepository.existsByUsername(updateCommand.username())).willReturn(false);
            given(binaryContentService.create(newFile)).willReturn(Optional.of(newProfileContent));
            given(userRepository.save(user)).willReturn(user);
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            UserDto result = userService.update(userId, updateCommand, newFile);

            // then
            assertThat(result).isEqualTo(expectedDto);

            // 사용자 정보와 프로필이 새 요청 값으로 변경됐는지 확인한다.
            assertThat(user.getUsername()).isEqualTo(updateCommand.username());
            assertThat(user.getEmail()).isEqualTo(updateCommand.email());
            assertThat(user.getProfile()).isEqualTo(newProfileContent);

            verify(userRepository).findById(userId);
            verify(userRepository).existsByEmail(updateCommand.email());
            verify(userRepository).existsByUsername(updateCommand.username());
            verify(binaryContentService).create(newFile);
            verify(userRepository).save(user);

            // 새 프로필로 교체됐으므로 기존 프로필 파일은 삭제되어야 한다.
            verify(binaryContentService).delete(oldProfile);

            verify(userMapper).toDto(user);

        }


        @Test
        @DisplayName("사용자 수정 성공 - 새 프로필 이미지가 없으면 기존 이미지를 유지")
        void update_keepsExistingProfileImage_whenNewProfileFileDoesNotExist() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 기존 프로필 이미지가 있는 사용자가 새 파일 없이 수정될 때,
            // 기존 프로필 이미지가 유지되고 파일 삭제가 실행되지 않는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();

            // 기존 프로필 이미지를 가진 사용자를 준비한다.
            // binaryContentService.create(null)가 Optional.empty()를 반환하면 서비스는 이 기존 프로필을 그대로 사용한다.
            UUID profileId = UUID.randomUUID();
            BinaryContent profile = new BinaryContent("file", "contentType", 1000L);
            ReflectionTestUtils.setField(profile, "id", profileId);

            User user = new User(command, profile);

            UserUpdateCommand updateCommand = createUpdateCommand();

            BinaryContentDto profileDto = new BinaryContentDto(profileId, profile.getOriginalFileName(), profile.getSize(), profile.getContentType());
            UserDto expectedDto = createExpectedDto(userId, updateCommand, false, profileDto);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));
            given(userRepository.existsByEmail(updateCommand.email())).willReturn(false);
            given(userRepository.existsByUsername(updateCommand.username())).willReturn(false);

            // 새 파일이 없으므로 파일 생성 결과는 Optional.empty()다.
            // 서비스는 새 이미지 대신 기존 profile을 유지해야 한다.
            given(binaryContentService.create(null)).willReturn(Optional.empty());

            given(userRepository.save(user)).willReturn(user);
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            UserDto result = userService.update(userId, updateCommand, null);

            // then
            assertThat(result).isEqualTo(expectedDto);

            // 새 프로필이 없으므로 기존 프로필이 그대로 유지되어야 한다.
            assertThat(user.getProfile()).isEqualTo(profile);
            assertThat(user.getProfile()).isNotNull();

            // 사용자 기본 정보는 updateCommand 값으로 수정되어야 한다.
            assertThat(user.getUsername()).isEqualTo(updateCommand.username());
            assertThat(user.getEmail()).isEqualTo(updateCommand.email());

            verify(userRepository).findById(userId);
            verify(userRepository).existsByEmail(updateCommand.email());
            verify(userRepository).existsByUsername(updateCommand.username());
            verify(binaryContentService).create(null);
            verify(userRepository).save(user);

            // 기존 프로필을 그대로 유지하므로 파일 삭제는 실행되면 안 된다.
            verify(binaryContentService, never()).delete(any(BinaryContent.class));

            verify(userMapper).toDto(user);

        }

        @Test
        @DisplayName("사용자 수정 성공 - 이메일만 변경되면 이메일 중복만 검사")
        void update_checksOnlyEmailDuplicate_whenOnlyEmailChanged() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 사용자명은 그대로이고 이메일만 변경될 때,
            // 이메일 중복 검사만 실행되고 사용자명 중복 검사는 건너뛰는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 사용자명은 기존 값 그대로 사용하고 이메일은 새 값으로 변경한다.
            // 서비스는 변경된 이메일에 대해서만 중복 검사를 수행해야 한다.
            UserUpdateCommand updateCommand = new UserUpdateCommand(
                    command.username(),
                    "changePassword",
                    "changeEmail@gmail.com"
            );
            UserDto expectedDto = createExpectedDto(userId, updateCommand, false);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // 이메일만 변경되므로 이메일 중복 검사 결과만 준비한다.
            given(userRepository.existsByEmail(updateCommand.email())).willReturn(false);

            // 이 테스트는 프로필 이미지 변경 없이 사용자 정보만 수정하는 케이스다.
            given(binaryContentService.create(null)).willReturn(Optional.empty());
            given(userRepository.save(user)).willReturn(user);
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            UserDto result = userService.update(userId, updateCommand, null);

            // then
            assertThat(result).isEqualTo(expectedDto);

            // 이메일은 변경 요청 값으로 수정되어야 한다.
            assertThat(user.getEmail()).isEqualTo(updateCommand.email());

            // 사용자명은 기존 값이 유지되어야 한다.
            assertThat(user.getUsername()).isEqualTo(command.username());

            verify(userRepository).findById(userId);

            // 이메일만 변경됐으므로 이메일 중복 검사만 실행되어야 한다.
            verify(userRepository).existsByEmail(updateCommand.email());
            verify(userRepository, never()).existsByUsername(anyString());

            verify(binaryContentService).create(null);
            verify(userRepository).save(user);

            // 기존 프로필 이미지가 없으므로 파일 삭제는 실행되지 않아야 한다.
            verify(binaryContentService, never()).delete(any(BinaryContent.class));

            verify(userMapper).toDto(user);

        }

        @Test
        @DisplayName("사용자 수정 성공 - 사용자명만 변경되면 사용자명 중복만 검사")
        void update_checksOnlyUsernameDuplicate_whenOnlyUsernameChanged() {
            // given
            // update()는 먼저 userId로 수정 대상 사용자를 조회한다.
            // 이 테스트는 이메일은 그대로이고 사용자명만 변경될 때,
            // 사용자명 중복 검사만 실행되고 이메일 중복 검사는 건너뛰는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 이메일은 기존 값 그대로 사용하고 사용자명은 새 값으로 변경한다.
            // 서비스는 변경된 사용자명에 대해서만 중복 검사를 수행해야 한다.
            UserUpdateCommand updateCommand = new UserUpdateCommand(
                    "changeUsername",
                    "changePassword",
                    command.email()
            );
            UserDto expectedDto = createExpectedDto(userId, updateCommand, false);

            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // 사용자명만 변경되므로 사용자명 중복 검사 결과만 준비한다.
            given(userRepository.existsByUsername(updateCommand.username())).willReturn(false);

            // 이 테스트는 프로필 이미지 변경 없이 사용자 정보만 수정하는 케이스다.
            given(binaryContentService.create(null)).willReturn(Optional.empty());

            given(userRepository.save(user)).willReturn(user);
            given(userMapper.toDto(user)).willReturn(expectedDto);

            // when
            UserDto result = userService.update(userId, updateCommand, null);

            // then
            assertThat(result).isEqualTo(expectedDto);

            // 사용자명은 변경 요청 값으로 수정되어야 한다.
            assertThat(user.getUsername()).isEqualTo(updateCommand.username());

            // 이메일은 기존 값이 유지되어야 한다.
            assertThat(user.getEmail()).isEqualTo(command.email());

            verify(userRepository).findById(userId);

            // 사용자명만 변경됐으므로 사용자명 중복 검사만 실행되어야 한다.
            verify(userRepository).existsByUsername(updateCommand.username());
            verify(userRepository, never()).existsByEmail(anyString());

            verify(binaryContentService).create(null);
            verify(userRepository).save(user);

            // 기존 프로필 이미지가 없으므로 파일 삭제는 실행되지 않아야 한다.
            verify(binaryContentService, never()).delete(any(BinaryContent.class));

            verify(userMapper).toDto(user);

        }

    }

    @Nested
    @DisplayName("사용자 삭제")
    class DeleteUserTest {

        @Test
        @DisplayName("사용자 삭제 성공")
        void delete_deletesUser_whenUserExists() {
            // given
            // delete()는 먼저 userId로 삭제 대상 사용자를 조회한다.
            // 삭제 성공 케이스이므로 repository가 User를 반환하도록 준비한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();
            User user = new User(command, null);

            // 서비스는 조회된 user.getId()로 deleteById()를 호출한다.
            // 직접 생성한 엔티티는 id가 없을 수 있으므로 테스트에서 id를 맞춰준다.
            ReflectionTestUtils.setField(user, "id", userId);

            // userId로 조회하면 삭제 대상 사용자가 존재하는 상황을 만든다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userService.delete(userId);

            // then
            // 삭제 대상 사용자를 조회했는지 확인한다.
            verify(userRepository).findById(userId);

            // 사용자 삭제 전에 연결된 부가 정보를 정리하는지 확인한다.
            verify(userStatusService).delete(user.getStatusId(), userId);
            verify(readStatusService).deleteByUserId(userId);
            verify(messageService).detachByAuthorId(userId);

            // 최종적으로 사용자 삭제가 실행됐는지 확인한다.
            verify(userRepository).deleteById(userId);

            // 프로필 이미지가 없는 사용자이므로 파일 삭제는 실행되지 않아야 한다.
            verify(binaryContentService, never()).delete(any(BinaryContent.class));

        }

        @Test
        @DisplayName("사용자 삭제 실패 - 존재하지 않는 사용자")
        void delete_throwsUserNotFoundException_whenUserDoesNotExist() {
            // given
            // delete()는 가장 먼저 userId로 삭제 대상 사용자를 조회한다.
            // 존재하지 않는 사용자 삭제 요청을 검증하기 위해 임의의 userId를 준비한다.
            UUID userId = UUID.randomUUID();

            // userId로 조회했을 때 사용자가 없는 상황을 만든다.
            // Optional.empty()가 반환되면 서비스는 UserNotFoundException을 던져야 한다.
            given(userRepository.findById(userId)).willReturn(Optional.empty());

            // when & then
            // 삭제 대상 사용자가 없으면 삭제 로직은 실패해야 한다.
            assertThatThrownBy(() -> userService.delete(userId))
                    .isInstanceOf(UserNotFoundException.class);

            // 삭제 대상 사용자를 조회했는지 확인한다.
            verify(userRepository).findById(userId);

            // 조회 단계에서 UserNotFoundException이 발생했으므로,
            // 사용자와 연결된 부가 정보를 정리하는 서비스들은 아예 호출되지 않아야 한다.
            verifyNoInteractions(userStatusService, readStatusService, messageService, binaryContentService);

            // 삭제 대상 사용자를 찾지 못했으므로,
            // 실제 사용자 삭제를 수행하는 deleteById()도 호출되면 안 된다.
            verify(userRepository, never()).deleteById(any(UUID.class));
        }

        @Test
        @DisplayName("사용자 삭제 성공 - 프로필 이미지가 있으면 파일도 삭제")
        void delete_deletesProfileImage_whenUserHasProfile() {
            // given
            // delete()는 먼저 userId로 삭제 대상 사용자를 조회한다.
            // 이 테스트는 프로필 이미지가 있는 사용자를 삭제할 때 파일 삭제까지 실행되는지 확인한다.
            UserCreateCommand command = createTestUserCommand();
            UUID userId = UUID.randomUUID();

            // 프로필 이미지가 있는 사용자 상황을 만들기 위해 BinaryContent를 준비한다.
            // user.isProfileImageExist()가 true가 되어야 binaryContentService.delete(...) 분기가 실행된다.
            BinaryContent profile = new BinaryContent("filename", "contentType", 1000L);
            User user = new User(command, profile);

            // 서비스는 조회된 user.getId()로 userRepository.deleteById()를 호출한다.
            // 직접 생성한 엔티티는 id가 없으므로 테스트에서 userId를 넣어 실제 삭제 흐름과 맞춘다.
            ReflectionTestUtils.setField(user, "id", userId);

            // userId로 조회하면 프로필 이미지가 있는 사용자가 반환되는 상황을 만든다.
            given(userRepository.findById(userId)).willReturn(Optional.of(user));

            // when
            userService.delete(userId);

            // then
            // 삭제 대상 사용자를 조회했는지 확인한다.
            verify(userRepository).findById(userId);

            // 사용자 삭제 전에 연결된 상태, 읽음 상태, 메시지 작성자 참조를 정리했는지 확인한다.
            verify(userStatusService).delete(user.getStatusId(), userId);
            verify(readStatusService).deleteByUserId(userId);
            verify(messageService).detachByAuthorId(userId);

            // 최종적으로 사용자 엔티티 삭제가 실행됐는지 확인한다.
            verify(userRepository).deleteById(userId);

            // 프로필 이미지가 있는 사용자이므로 해당 프로필 파일 삭제가 실행되어야 한다.
            verify(binaryContentService).delete(profile);

        }
    }


    private UserCreateCommand createTestUserCommand() {
        String s = retrieveShortUUID();
        return new UserCreateCommand("test" + s, "test", "test" + s + "@gamil.com");
    }

    private String retrieveShortUUID() {
        return UUID.randomUUID().toString().substring(0, 4);
    }

    private UserDto createExpectedDto(UUID userId, UserCreateCommand command, boolean isOnline) {
        return new UserDto(
                userId,
                command.username(),
                command.email(),
                null,
                isOnline,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }


    private UserDto createExpectedDto(UUID userId, UserUpdateCommand updateCommand, boolean isOnline) {
        return createExpectedDto(userId, updateCommand, isOnline, null);
    }

    private UserDto createExpectedDto(UUID userId, UserUpdateCommand updateCommand, boolean isOnline, BinaryContentDto profile) {
        return new UserDto(
                userId,
                updateCommand.username(),
                updateCommand.email(),
                profile,
                isOnline,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }


    private UserDto createExpectedDto(UserCreateCommand command) {
        return createExpectedDto(UUID.randomUUID(), command, false);
    }

    private UserStatusDto createUserStatusDto(UUID userId) {
        return new UserStatusDto(
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now(),
                userId,
                OffsetDateTime.now()
        );
    }

    private UserUpdateCommand createUpdateCommand() {
        return new UserUpdateCommand("changeUsername", "changePassword", "changeEmail@gmail.com");
    }


}
