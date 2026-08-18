package com.loganalyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.dto.request.LiveIngestionRequest;
import com.loganalyzer.dto.response.ApiTokenResponse;
import com.loganalyzer.dto.response.AuthResponse;
import com.loganalyzer.dto.response.LiveIngestionResponse;
import com.loganalyzer.dto.response.LogSourceResponse;
import com.loganalyzer.entity.User;
import com.loganalyzer.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "aeip.redis.required=false",
                "server.servlet.context-path=/api"
        }
)
class RealtimeSseIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SseEmitterRegistry sseEmitterRegistry;

    @MockBean
    private IngestionDedupService ingestionDedupService;

    @Test
    void liveIngestionPublishesLogIngestedToOpenSseConnection()
            throws Exception {

        TestIdentity identity = registerUser();
        String apiToken = createApiToken(identity.jwt());
        LogSourceResponse source = createLogSource(identity.jwt());
        User user = userRepository.findByUsername(identity.username()).orElseThrow();

        CountDownLatch connected = new CountDownLatch(1);
        CountDownLatch logIngested = new CountDownLatch(1);
        AtomicReference<JsonNode> logIngestedPayload = new AtomicReference<>();
        AtomicReference<Throwable> readerFailure = new AtomicReference<>();

        HttpURLConnection connection = openSseConnection(
                identity.jwt(),
                connected,
                logIngested,
                logIngestedPayload,
                readerFailure
        );

        ExecutorService readerExecutor = Executors.newSingleThreadExecutor();
        try {
            readerExecutor.submit(() -> readSseStream(
                    connection,
                    connected,
                    logIngested,
                    logIngestedPayload,
                    readerFailure
            ));

            assertThat(connected.await(5, TimeUnit.SECONDS)).isTrue();
            assertThat(sseEmitterRegistry.activeCount(user.getId())).isEqualTo(1);

            String hash = "sse-test-" + UUID.randomUUID();
            when(ingestionDedupService.computeBatchHash(
                    anyLong(),
                    any(LiveIngestionRequest.class)
            )).thenReturn(hash);
            when(ingestionDedupService.claimBatch(hash)).thenReturn(true);

            LiveIngestionResponse ingestionResponse =
                    submitUniqueIngestion(apiToken, source.getId());

            assertThat(ingestionResponse.isDuplicate()).isFalse();
            assertThat(ingestionResponse.getAcceptedLines()).isEqualTo(2);
            assertThat(ingestionResponse.getProcessedLines()).isEqualTo(2);

            assertThat(logIngested.await(10, TimeUnit.SECONDS)).isTrue();
            assertThat(readerFailure.get()).isNull();

            JsonNode event = logIngestedPayload.get();
            assertThat(event.path("type").asText()).isEqualTo("LOG_INGESTED");
            assertThat(event.path("data").path("sourceId").asLong())
                    .isEqualTo(source.getId());
            assertThat(event.path("data").path("count").asInt()).isEqualTo(2);
        } finally {
            connection.disconnect();
            readerExecutor.shutdownNow();
        }
    }

    private TestIdentity registerUser() {

        String unique = UUID.randomUUID().toString().replace("-", "");
        String username = "sse_" + unique.substring(0, 12);
        String password = "Password@123";

        ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
                "/auth/register",
                Map.of(
                        "displayName", "SSE Test User",
                        "username", username,
                        "email", username + "@example.com",
                        "password", password
                ),
                AuthResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).isNotBlank();

        return new TestIdentity(username, response.getBody().getToken());
    }

    private String createApiToken(String jwt) {

        ResponseEntity<ApiTokenResponse> response = restTemplate.exchange(
                "/settings/tokens",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of("name", "sse-integration-test"),
                        bearer(jwt)
                ),
                ApiTokenResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getToken()).startsWith("logai_live_");

        return response.getBody().getToken();
    }

    private LogSourceResponse createLogSource(String jwt) {

        ResponseEntity<LogSourceResponse> response = restTemplate.exchange(
                "/log-sources",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of(
                                "sourceName", "SSE Integration Test",
                                "sourceType", "WATCHER"
                        ),
                        bearer(jwt)
                ),
                LogSourceResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isNotNull();

        return response.getBody();
    }

    private LiveIngestionResponse submitUniqueIngestion(
            String apiToken,
            Long sourceId
    ) {

        String unique = UUID.randomUUID().toString();
        ResponseEntity<LiveIngestionResponse> response = restTemplate.exchange(
                "/ingest/stream",
                HttpMethod.POST,
                new HttpEntity<>(
                        Map.of(
                                "sourceId", sourceId,
                                "batchTimestamp", Instant.now().toString(),
                                "lines", List.of(
                                        "2026-08-18 19:25:10 ERROR [AuthService] NullPointerException " + unique,
                                        "2026-08-18 19:25:12 WARN [PaymentService] Payment gateway timeout " + unique
                                )
                        ),
                        bearer(apiToken)
                ),
                LiveIngestionResponse.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();

        return response.getBody();
    }

    private HttpURLConnection openSseConnection(
            String jwt,
            CountDownLatch connected,
            CountDownLatch logIngested,
            AtomicReference<JsonNode> logIngestedPayload,
            AtomicReference<Throwable> readerFailure
    ) throws Exception {

        String encodedToken = URLEncoder.encode(jwt, StandardCharsets.UTF_8);
        URI uri = URI.create(
                "http://localhost:" + port + "/api/events/stream?token="
                        + encodedToken
        );

        HttpURLConnection connection =
                (HttpURLConnection) uri.toURL().openConnection();
        connection.setRequestMethod("GET");
        connection.setRequestProperty("Accept", MediaType.TEXT_EVENT_STREAM_VALUE);
        connection.setReadTimeout(15_000);
        connection.setConnectTimeout(5_000);

        int status = connection.getResponseCode();
        assertThat(status).isEqualTo(HttpStatus.OK.value());
        assertThat(connection.getContentType()).contains("text/event-stream");

        return connection;
    }

    private void readSseStream(
            HttpURLConnection connection,
            CountDownLatch connected,
            CountDownLatch logIngested,
            AtomicReference<JsonNode> logIngestedPayload,
            AtomicReference<Throwable> readerFailure
    ) {

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        connection.getInputStream(),
                        StandardCharsets.UTF_8
                )
        )) {
            String eventName = null;
            String data = null;
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.startsWith("event:")) {
                    eventName = line.substring("event:".length()).trim();
                    continue;
                }

                if (line.startsWith("data:")) {
                    data = line.substring("data:".length()).trim();
                    continue;
                }

                if (!line.isBlank() || eventName == null) {
                    continue;
                }

                if ("CONNECTED".equals(eventName)) {
                    connected.countDown();
                }

                if ("LOG_INGESTED".equals(eventName)) {
                    logIngestedPayload.set(objectMapper.readTree(data));
                    logIngested.countDown();
                    return;
                }

                eventName = null;
                data = null;
            }
        } catch (Throwable throwable) {
            if (logIngested.getCount() > 0) {
                readerFailure.set(throwable);
            }
        }
    }

    private HttpHeaders bearer(String token) {

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private record TestIdentity(String username, String jwt) {
    }
}
