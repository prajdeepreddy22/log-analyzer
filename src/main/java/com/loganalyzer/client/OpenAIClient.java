package com.loganalyzer.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

            log.info("OpenAI key loaded={}", apiKey != null && !apiKey.isBlank());

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

                    throw new RuntimeException(
                            "OpenAI API failed: " +
                                    response.code() +
                                    " " +
                                    response.message()
                    );
                }

                ResponseBody responseBodyObj = response.body();

                if (responseBodyObj == null) {

                    throw new RuntimeException(
                            "Empty OpenAI response"
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

                    throw new RuntimeException(
                            "No AI choices returned"
                    );
                }

                Map<?, ?> choice =
                        (Map<?, ?>) choices.get(0);

                Map<?, ?> message =
                        (Map<?, ?>) choice.get("message");

                String content =
                        message.get("content").toString();

                log.info(
                        "AI raw response received length={}",
                        content.length()
                );

                return extractJson(content);

            }

        } catch (Exception e) {

            log.error("OpenAI analyzeLogs failed", e);

            throw new RuntimeException(
                    "OpenAI analyze failed",
                    e
            );
        }
    }

    // =====================================================
    // CHAT QUESTION
    // =====================================================
    public String askQuestion(String prompt) {

        try {

            log.info("OpenAI key loaded={}", apiKey != null && !apiKey.isBlank());

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

                    throw new RuntimeException(
                            "OpenAI API failed"
                    );
                }

                ResponseBody responseBodyObj = response.body();

                if (responseBodyObj == null) {

                    throw new RuntimeException(
                            "Empty OpenAI response"
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

                    throw new RuntimeException(
                            "No AI choices returned"
                    );
                }

                Map<?, ?> choice =
                        (Map<?, ?>) choices.get(0);

                Map<?, ?> message =
                        (Map<?, ?>) choice.get("message");

                return message.get("content").toString();
            }

        } catch (Exception e) {

            log.error("askQuestion failed", e);

            return """
                    AI service temporarily unavailable.

                    Please retry later.
                    """;
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

                throw new RuntimeException(
                        "JSON not found in AI response"
                );
            }

            String json =
                    content.substring(start, end + 1);

            return objectMapper.readValue(
                    json,
                    new TypeReference<>() {}
            );

        } catch (Exception e) {

            log.error("Failed parsing AI JSON", e);

            return fallbackAnalysis(e);
        }
    }

    // =====================================================
    // FALLBACK RESPONSE
    // =====================================================
    private Map<String, Object> fallbackAnalysis(Exception e) {

        return Map.of(
                "summary",
                "AI analysis failed",

                "root_cause",
                "Unable to determine root cause",

                "developer_mistake",
                "Insufficient AI response",

                "fix_suggestion",
                "Retry analysis or inspect logs manually",

                "code_fix",
                "N/A",

                "severity_score",
                1
        );
    }
}