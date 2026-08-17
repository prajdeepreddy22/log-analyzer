package com.loganalyzer.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateApiTokenRequest {

    @Size(max = 255, message = "must be 255 characters or fewer")
    private String name;
}
