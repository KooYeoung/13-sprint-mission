package com.sprint.mission.discodeit.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprint.mission.discodeit.dto.command.channel.ChannelCreatePublicCommand;
import com.sprint.mission.discodeit.dto.command.user.UserCreateCommand;
import com.sprint.mission.discodeit.entity.BinaryContentStatus;
import com.sprint.mission.discodeit.entity.Channel;
import com.sprint.mission.discodeit.entity.ChannelType;
import com.sprint.mission.discodeit.repository.BinaryContentRepository;
import com.sprint.mission.discodeit.repository.ChannelRepository;
import com.sprint.mission.discodeit.service.BinaryContentStorage;
import com.sprint.mission.discodeit.service.UserService;
import com.sprint.mission.discodeit.storage.BinaryContentUpload;
import com.sprint.mission.discodeit.support.AuthenticatedHttpClient;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.sprint.mission.discodeit.support.AsyncTestSupport.awaitIdle;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:h2:mem:async-http-response;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "discodeit.storage.type=local", "discodeit.async.enabled=true",
        "spring.servlet.multipart.file-size-threshold=0B"
})
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AsyncMessageResponseIntegrationTest {
    @LocalServerPort int port;
    @Autowired UserService users;
    @Autowired ChannelRepository channels;
    @Autowired BinaryContentRepository binaries;
    @Autowired ObjectMapper mapper;
    @Autowired ThreadPoolTaskExecutor fileUploadExecutor;
    @Autowired ThreadPoolTaskExecutor notificationExecutor;
    @MockitoBean BinaryContentStorage storage;

    @Test
    void httpResponsePrecedesUploadCompletionAndRequestContextReachesWorker() throws Exception {
        var author = users.create(new UserCreateCommand("http-author", "password123!", "author@http.local"), null);
        var channel = channels.saveAndFlush(new Channel(new ChannelCreatePublicCommand("http-channel", "test", ChannelType.PUBLIC)));
        var client = new AuthenticatedHttpClient(port);
        client.login("http-author", "password123!", mapper);
        String json = mapper.writeValueAsString(Map.of("content", "hello", "authorId", author.id(), "channelId", channel.getId()));
        String multipart = "--boundary\r\nContent-Disposition: form-data; name=\"messageCreateRequest\"\r\nContent-Type: application/json\r\n\r\n"
                + json + "\r\n--boundary\r\nContent-Disposition: form-data; name=\"attachments\"; filename=\"file.txt\"\r\nContent-Type: text/plain\r\n\r\nbody\r\n--boundary--\r\n";
        var request = client.post("/api/messages", "multipart/form-data; boundary=boundary", multipart.getBytes(StandardCharsets.UTF_8));
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var requestId = new AtomicReference<String>();
        var bytes = new AtomicReference<byte[]>();
        given(storage.put(any(), any())).willAnswer(invocation -> {
            assertThat(Thread.currentThread().getName()).startsWith("file-upload");
            assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("http-author");
            requestId.set(MDC.get("requestId"));
            started.countDown();
            try (var stream = invocation.<BinaryContentUpload>getArgument(1).inputStream()) {
                assertThat(release.await(10, TimeUnit.SECONDS)).isTrue();
                bytes.set(stream.readAllBytes());
            }
            return invocation.getArgument(0);
        });
        var sender = Executors.newSingleThreadExecutor();
        try {
            var future = sender.submit(() -> client.send(request));
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            var response = future.get(5, TimeUnit.SECONDS);
            assertThat(response.statusCode()).isEqualTo(201);
            assertThat(response.headers().firstValue("Discodeit-Request-ID")).contains(requestId.get());
            UUID fileId = UUID.fromString(mapper.readTree(response.body()).path("attachments").get(0).path("id").asText());
            assertThat(binaries.findById(fileId).orElseThrow().getStatus()).isEqualTo(BinaryContentStatus.PROCESSING);
            release.countDown();
            await().untilAsserted(() -> assertThat(binaries.findById(fileId).orElseThrow().getStatus()).isEqualTo(BinaryContentStatus.SUCCESS));
            assertThat(bytes.get()).isEqualTo("body".getBytes(StandardCharsets.UTF_8));
        } finally {
            release.countDown();
            sender.shutdownNow();
            awaitIdle(fileUploadExecutor, notificationExecutor);
        }
    }
}
