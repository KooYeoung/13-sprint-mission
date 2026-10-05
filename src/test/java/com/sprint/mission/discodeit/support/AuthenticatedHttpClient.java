package com.sprint.mission.discodeit.support;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/** 실제 로그인·CSRF·JWT 필터를 통과하는 HTTP 테스트 클라이언트. */
public final class AuthenticatedHttpClient {
    private final String baseUrl;
    private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
            .version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(10)).build();
    private String accessToken;

    public AuthenticatedHttpClient(int port) { baseUrl = "http://localhost:" + port; }

    public void login(String username, String password, ObjectMapper mapper) throws Exception {
        assertThat(get("/api/auth/csrf-token").statusCode()).isEqualTo(204);
        String form = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(password, StandardCharsets.UTF_8);
        var response = send(post("/api/auth/login", "application/x-www-form-urlencoded", form.getBytes(StandardCharsets.UTF_8)));
        assertThat(response.statusCode()).isEqualTo(200);
        accessToken = mapper.readTree(response.body()).path("accessToken").asText();
        assertThat(accessToken).isNotBlank();
        assertThat(get("/api/auth/csrf-token").statusCode()).isEqualTo(204);
    }

    public HttpRequest post(String path, String contentType, byte[] body) {
        String csrf = cookies.getCookieStore().getCookies().stream()
                .filter(cookie -> cookie.getName().equals("XSRF-TOKEN")).findFirst().orElseThrow().getValue();
        return request(path).header("X-XSRF-TOKEN", csrf).header("Content-Type", contentType)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build();
    }

    public HttpResponse<byte[]> get(String path) throws Exception { return send(request(path).GET().build()); }

    public HttpResponse<byte[]> send(HttpRequest request) throws Exception {
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private HttpRequest.Builder request(String path) {
        var builder = HttpRequest.newBuilder(URI.create(baseUrl + path)).timeout(Duration.ofSeconds(20));
        if (accessToken != null) builder.header("Authorization", "Bearer " + accessToken);
        return builder;
    }
}
