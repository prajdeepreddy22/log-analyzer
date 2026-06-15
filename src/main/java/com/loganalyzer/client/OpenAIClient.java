package com.loganalyzer.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.loganalyzer.exception.AIProviderException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OpenAIClient {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.model:gpt-4o-mini}")
    private String model;

    private final ObjectMapper objectMapper;

    private final OkHttpClient client;

    // =====================================================
    // ANALYZE LOGS
    // =====================================================
    public Map<String, Object> analyzeLogs(String prompt) {

        try {

            String systemPrompt = """
                    You are a senior production incident investigator.

                    Analyze logs carefully.

                    Return ONLY valid JSON.

                    Example:
                    {
                      "summary": "...",
                      "root_cause": "...",
                      "developer_mistake": "...",
                      "fix_suggestion": "...",
                      "code_fix": "...",
                      "severity_score": 4
                    }

                    Rules:
                    - Do not hallucinate
                    - Use log evidence
                    - Focus on actual errors/exceptions
                    - If insufficient data mention it
                    """;

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("model", model);

            requestBody.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content", systemPrompt
                    ),
                    Map.of(
                            "role", "user",
                            "content", prompt
                    )
            ));

            requestBody.put("temperature", 0.2);

            log.info("Calling OpenAI model={}", model);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    throw providerHttpException(response);
                }

                ResponseBody responseBodyObj = response.body();

                if (responseBodyObj == null) {

                    throw new AIProviderException(
                            "OpenAI returned an empty response",
                            true,
                            response.code()
                    );
                }

                String responseBody =
                        responseBodyObj.string();

                log.info("OpenAI response success");

                Map<String, Object> responseMap =
                        objectMapper.readValue(
                                responseBody,
                                new TypeReference<>() {}
                        );

                List<?> choices =
                        (List<?>) responseMap.get("choices");

                if (choices == null || choices.isEmpty()) {

                    throw new AIProviderException(
                            "OpenAI returned no choices",
                            true,
                            response.code()
                    );
                }

                Map<?, ?> choice =
                        (Map<?, ?>) choices.get(0);

                Object messageValue = choice.get("message");

                if (!(messageValue instanceof Map<?, ?> message)
                        || message.get("content") == null) {
                    throw new AIProviderException(
                            "OpenAI returned an invalid message",
                            true,
                            response.code()
                    );
                }

                String content = message.get("content").toString();

                log.info(
                        "AI raw response received length={}",
                        content.length()
                );

                return extractJson(content);

            }

        } catch (AIProviderException e) {
            throw e;
        } catch (Exception e) {

            log.error(
                    "OpenAI analyzeLogs failed type={}",
                    e.getClass().getSimpleName()
            );

            throw new AIProviderException(
                    "OpenAI analysis request failed",
                    e,
                    true,
                    null
            );
        }
    }

    // =====================================================
    // CHAT QUESTION
    // =====================================================
    public String askQuestion(String prompt) {

        try {

            Map<String, Object> requestBody = new HashMap<>();

            requestBody.put("model", model);

            requestBody.put("messages", List.of(
                    Map.of(
                            "role", "system",
                            "content",
                            """
                            You are a senior backend engineer.

                            Answer based ONLY on logs.

                            Be concise and technical.
                            """
                    ),
                    Map.of(
                            "role", "user",
                            "content", prompt
                    )
            ));

            requestBody.put("temperature", 0.3);

            log.info("Calling OpenAI chat model={}", model);

            RequestBody body = RequestBody.create(
                    objectMapper.writeValueAsString(requestBody),
                    MediaType.parse("application/json")
            );

            Request request = new Request.Builder()
                    .url("https://api.openai.com/v1/chat/completions")
                    .addHeader("Authorization", "Bearer " + apiKey)
                    .addHeader("Content-Type", "application/json")
                    .post(body)
                    .build();

            try (Response response = client.newCall(request).execute()) {

                if (!response.isSuccessful()) {
                    throw providerHttpException(response);
                }

                ResponseBody responseBodyObj = response.body();

                if (responseBodyObj == null) {

                    throw new AIProviderException(
                            "OpenAI returned an empty response",
                            true,
                            response.code()
                    );
                }

                String responseBody =
                        responseBodyObj.string();

                log.info("OpenAI chat response success");

                Map<String, Object> responseMap =
                        objectMapper.readValue(
                                responseBody,
                                new TypeReference<>() {}
                        );

                List<?> choices =
                        (List<?>) responseMap.get("choices");

                if (choices == null || choices.isEmpty()) {

                    throw new AIProviderException(
                            "OpenAI returned no choices",
                            true,
                            response.code()
                    );
                }

                Map<?, ?> choice =
                        (Map<?, ?>) choices.get(0);

                Object messageValue = choice.get("message");

                if (!(messageValue instanceof Map<?, ?> message)
                        || message.get("content") == null) {
                    throw new AIProviderException(
                            "OpenAI returned an invalid message",
                            true,
                            response.code()
                    );
                }

                return message.get("content").toString();
            }

        } catch (AIProviderException e) {
            throw e;
        } catch (Exception e) {

            log.error(
                    "OpenAI chat failed type={}",
                    e.getClass().getSimpleName()
            );

            throw new AIProviderException(
                    "OpenAI chat request failed",
                    e,
                    true,
                    null
            );
        }
    }

    // =====================================================
    // STREAMING
    // =====================================================
    public String streamQuestion(String prompt) {

        return askQuestion(prompt);
    }

    // =====================================================
    // JSON EXTRACTION
    // =====================================================
    private Map<String, Object> extractJson(String content) {

        try {

            int start = content.indexOf("{");
            int end = content.lastIndexOf("}");

            if (start == -1 || end == -1) {

                throw new AIProviderException(
                        "JSON not found in AI response",
                        true,
                        null
                );
            }

            String json =
                    content.substring(start, end + 1);

            return objectMapper.readValue(
                    json,
                    new TypeReference<>() {}
            );

        } catch (AIProviderException e) {
            throw e;
        } catch (Exception e) {

            log.error(
                    "Failed parsing AI JSON type={}",
                    e.getClass().getSimpleName()
            );

            throw new AIProviderException(
                    "OpenAI returned malformed analysis JSON",
                    e,
                    true,
                    null
            );
        }
    }

    private AIProviderException providerHttpException(Response response) {

        int status = response.code();
        boolean retryable = status == 408
                || status == 409
                || status == 429
                || status >= 500;

        return new AIProviderException(
                "OpenAI API request failed with status " + status,
                retryable,
                status
        );
    }
}
