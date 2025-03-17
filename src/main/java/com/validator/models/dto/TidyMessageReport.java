package com.validator.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class TidyMessageReport {
    private String level;
    private String message;
    private int line;
    private int column;
}
