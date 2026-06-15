package com.loganalyzer.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.exception.AIProviderException;
import okhttp3.Call;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OpenAIClientTest {

    @Test
    void classifiesRateLimitAsRetryable() throws Exception {
        OpenAIClient client = clientReturning(response(429, "{}"));

        assertThatThrownBy(() -> client.analyzeLogs("prompt"))
                .isInstanceOfSatisfying(
                        AIProviderException.class,
                        exception -> {
                            org.assertj.core.api.Assertions.assertThat(
                                    exception.isRetryable()).isTrue();
                            org.assertj.core.api.Assertions.assertThat(
                                    exception.getProviderStatus()).isEqualTo(429);
                        }
                );
    }

    @Test
    void classifiesAuthenticationFailureAsNonRetryable() throws Exception {
        OpenAIClient client = clientReturning(response(401, "{}"));

        assertThatThrownBy(() -> client.analyzeLogs("prompt"))
                .isInstanceOfSatisfying(
                        AIProviderException.class,
                        exception -> {
                            org.assertj.core.api.Assertions.assertThat(
                                    exception.isRetryable()).isFalse();
                            org.assertj.core.api.Assertions.assertThat(
                                    exception.getProviderStatus()).isEqualTo(401);
                        }
                );
    }

    @Test
    void rejectsMalformedAnalysisInsteadOfReturningFallbackSuccess()
            throws Exception {

        String body = """
                {
                  "choices": [
                    {"message": {"content": "not-json"}}
                  ]
                }
                """;

        OpenAIClient client = clientReturning(response(200, body));

        assertThatThrownBy(() -> client.analyzeLogs("prompt"))
                .isInstanceOf(AIProviderException.class)
                .hasMessageContaining("JSON");
    }

    private OpenAIClient clientReturning(Response response) throws Exception {
        OkHttpClient httpClient = mock(OkHttpClient.class);
        Call call = mock(Call.class);
        when(httpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(response);

        OpenAIClient client =
                new OpenAIClient(new ObjectMapper(), httpClient);
        ReflectionTestUtils.setField(client, "apiKey", "test-key");
        ReflectionTestUtils.setField(client, "model", "test-model");
        return client;
    }

    private Response response(int status, String body) {
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/chat/completions")
                .build();

        return new Response.Builder()
                .request(request)
                .protocol(Protocol.HTTP_1_1)
                .code(status)
                .message("test")
                .body(ResponseBody.create(
                        body,
                        MediaType.parse("application/json")
                ))
                .build();
    }
}
