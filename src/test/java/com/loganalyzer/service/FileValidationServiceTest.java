package com.loganalyzer.service;

import com.loganalyzer.exception.FileValidationException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileValidationServiceTest {

    private final FileValidationService service =
            new FileValidationService();

    @Test
    void acceptsLogAndTextExtensionsCaseInsensitively() {
        service.validate(file("application.LOG"));
        service.validate(file("application.Txt"));
    }

    @Test
    void rejectsUnsupportedExtensionWithReason() {
        assertThatThrownBy(() -> service.validate(file("application.json")))
                .isInstanceOf(FileValidationException.class)
                .hasMessage("Only .log and .txt allowed");
    }

    private MockMultipartFile file(String name) {
        return new MockMultipartFile(
                "file",
                name,
                "text/plain",
                "sample log".getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }
}
