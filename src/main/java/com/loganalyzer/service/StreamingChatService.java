package com.loganalyzer.service;

import com.loganalyzer.dto.request.ChatRequest;
import com.loganalyzer.dto.response.ChatResponse;
import com.loganalyzer.dto.response.StreamChunkResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreamingChatService {

    private static final long RECONNECT_SUPPRESSION_MILLIS = 120000L;

    private final ChatService chatService;

    private final Set<String> activeStreams =
            ConcurrentHashMap.newKeySet();

    private final Map<String, Long> completedStreams =
            new ConcurrentHashMap<>();

    public boolean tryStartStream(
            String message,
            String uploadId,
            Long userId
    ) {

        cleanupCompletedStreams();

        String key = streamKey(message, uploadId, userId);
        Long completedUntil = completedStreams.get(key);
        long now = Instant.now().toEpochMilli();

        if (completedUntil != null && completedUntil > now) {
            return false;
        }

        return activeStreams.add(key);
    }

    // =====================================================
    // ENTRY POINT
    // =====================================================
    public SseEmitter streamResponse(
            String message,
            String uploadId,
            Long userId
    ) {

        // 0L = no timeout
        SseEmitter emitter = new SseEmitter(300000L);

        streamAsync(
                message,
                uploadId,
                userId,
                emitter
        );

        return emitter;
    }

    // =====================================================
    // ASYNC STREAMING
    // =====================================================
    @Async("aiExecutor")
    public void streamAsync(
            String message,
            String uploadId,
            Long userId,
            SseEmitter emitter
    ) {

        try {
            String streamKey = streamKey(message, uploadId, userId);

            log.info(
                    "Starting AI stream userId={} uploadId={} question={}",
                    userId,
                    uploadId,
                    message
            );

            // =====================================================
            // BUILD CHAT REQUEST
            // =====================================================
            ChatRequest request = ChatRequest.builder()
                    .question(message)
                    .uploadId(uploadId)
                    .build();

            // =====================================================
            // USE FULL CHAT PIPELINE
            // =====================================================
            ChatResponse chatResponse =
                    chatService.askQuestion(
                            request,
                            userId
                    );

            String answer = chatResponse.getAnswer();

            // =====================================================
            // STREAM WORD BY WORD
            // =====================================================
            String[] chunks =
                    answer.split("(?<=\\n\\n)|(?<=[.!?])\\s+");

            for (String chunk : chunks) {

                if (chunk.isBlank()) {
                    continue;
                }

                StreamChunkResponse response =
                        StreamChunkResponse.builder()
                                .content(chunk)
                                .completed(false)
                                .build();

                emitter.send(
                        SseEmitter.event()
                                .name("message")
                                .data(response)
                );

                Thread.sleep(60);
            }



            // =====================================================
            // FINAL COMPLETION EVENT
            // =====================================================
            emitter.send(
                    SseEmitter.event()
                            .name("complete")
                            .data(
                                    StreamChunkResponse.builder()
                                            .content("")
                                            .completed(true)
                                            .build()
                            )
            );

            emitter.complete();
            markCompleted(streamKey);

            log.info(
                    "AI stream completed userId={} uploadId={}",
                    userId,
                    uploadId
            );

        } catch (Exception e) {

            log.error(
                    "Streaming failed userId={} uploadId={}",
                    userId,
                    uploadId,
                    e
            );

            try {

                emitter.send(
                        SseEmitter.event()
                                .name("error")
                                .data(
                                        StreamChunkResponse.builder()
                                                .content(
                                                        "AI streaming failed"
                                                )
                                                .completed(true)
                                                .build()
                                )
                );

            } catch (Exception ignored) {
            }

            emitter.completeWithError(e);
        } finally {

            activeStreams.remove(
                    streamKey(message, uploadId, userId)
            );
        }
    }

    private void markCompleted(String streamKey) {

        completedStreams.put(
                streamKey,
                Instant.now().toEpochMilli()
                        + RECONNECT_SUPPRESSION_MILLIS
        );
    }

    private void cleanupCompletedStreams() {

        long now = Instant.now().toEpochMilli();

        completedStreams.entrySet().removeIf(
                entry -> entry.getValue() <= now
        );
    }

    private String streamKey(
            String message,
            String uploadId,
            Long userId
    ) {

        String normalizedMessage = message == null
                ? ""
                : message.trim().toLowerCase().replaceAll("\\s+", " ");

        return userId + "|"
                + (uploadId == null ? "" : uploadId)
                + "|"
                + normalizedMessage;
    }
}
