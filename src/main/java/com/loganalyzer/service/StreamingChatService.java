package com.loganalyzer.service;

import com.loganalyzer.client.OpenAIClient;
import com.loganalyzer.dto.response.StreamChunkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingChatService {

    private final OpenAIClient openAIClient;

    // =====================================================
    // ENTRY POINT — returns emitter immediately
    // actual streaming happens async
    // =====================================================
    public SseEmitter streamResponse(
            String message,
            String uploadId,  // optional — used for log-aware context
            Long userId
    ) {
        // 0L = no timeout (stream until complete or error)
        SseEmitter emitter = new SseEmitter(0L);

        streamAsync(message, uploadId, userId, emitter);

        return emitter;
    }

    // =====================================================
    // ASYNC STREAMING — runs in aiExecutor thread pool
    // Splits AI response into word chunks and sends via SSE
    // =====================================================
    @Async("aiExecutor")
    public void streamAsync(
            String message,
            String uploadId,
            Long userId,
            SseEmitter emitter
    ) {
        try {
            log.info("Starting AI stream userId={} uploadId={} message={}",
                    userId, uploadId, message);

            // Call AI — in Phase 2 this will use RAG with uploadId context
            String answer = openAIClient.streamQuestion(message);

            // Split into word chunks and stream with delay
            String[] chunks = answer.split(" ");

            for (String chunk : chunks) {

                StreamChunkResponse response = StreamChunkResponse.builder()
                        .content(chunk + " ")
                        .completed(false)
                        .build();

                emitter.send(response);

                // 40ms delay between chunks simulates real streaming
                Thread.sleep(40);
            }

            // Send completion signal
            emitter.send(
                    StreamChunkResponse.builder()
                            .content("")
                            .completed(true)
                            .build()
            );

            emitter.complete();

            log.info("AI stream completed userId={} uploadId={}", userId, uploadId);

        } catch (Exception e) {
            log.error("Streaming failed userId={} uploadId={}", userId, uploadId, e);
            emitter.completeWithError(e);
        }
    }
}