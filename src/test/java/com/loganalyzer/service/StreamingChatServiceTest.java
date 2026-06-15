package com.loganalyzer.service;

import com.loganalyzer.dto.response.ChatResponse;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StreamingChatServiceTest {

    @Test
    void completesNormallyAfterSendingSseErrorEvent() {
        ChatService chatService = mock(ChatService.class);
        when(chatService.askQuestion(any(), eq(1L)))
                .thenThrow(new RuntimeException("provider detail"));

        Executor directExecutor = Runnable::run;
        StreamingChatService service =
                new StreamingChatService(chatService, directExecutor);

        TrackingEmitter emitter = new TrackingEmitter();
        service.streamAsync("question", "upload-1", 1L, emitter);

        assertThat(emitter.completed).isTrue();
        assertThat(emitter.completedWithError).isFalse();
        assertThat(service.tryStartStream("question", "upload-1", 1L)).isFalse();
    }

    @Test
    void successfulStreamCompletesAndSuppressesImmediateReconnect() {
        ChatService chatService = mock(ChatService.class);
        when(chatService.askQuestion(any(), eq(1L)))
                .thenReturn(ChatResponse.builder().answer("Done.").build());

        StreamingChatService service =
                new StreamingChatService(chatService, Runnable::run);

        assertThat(service.tryStartStream("question", "upload-1", 1L)).isTrue();
        service.streamResponse("question", "upload-1", 1L);

        assertThat(service.tryStartStream("question", "upload-1", 1L)).isFalse();
    }

    @Test
    void timeoutErrorCompletionSuppressesImmediateReconnect() {
        StreamingChatService service =
                new StreamingChatService(mock(ChatService.class), Runnable::run);

        assertThat(service.tryStartStream("question", "upload-1", 1L)).isTrue();

        TrackingEmitter emitter = new TrackingEmitter();
        service.completeWithErrorEvent(
                emitter,
                "1|upload-1|question",
                "AI streaming timed out"
        );

        assertThat(emitter.completed).isTrue();
        assertThat(emitter.completedWithError).isFalse();
        assertThat(service.tryStartStream("question", "upload-1", 1L)).isFalse();
    }

    private static class TrackingEmitter extends SseEmitter {
        private boolean completed;
        private boolean completedWithError;

        @Override
        public synchronized void complete() {
            completed = true;
            super.complete();
        }

        @Override
        public synchronized void completeWithError(Throwable ex) {
            completedWithError = true;
            super.completeWithError(ex);
        }
    }
}
