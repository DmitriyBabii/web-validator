package com.validator.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@Builder
public class ElementStyle {
    private String outerHtml;
    private Map<String, String> styles;
}
