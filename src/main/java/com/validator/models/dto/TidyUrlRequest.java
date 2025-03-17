package com.validator.models.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TidyUrlRequest {
    @NotBlank
    private String url;
}
