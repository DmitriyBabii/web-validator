package com.validator.models.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
@AllArgsConstructor
public class ColorAnalyzeElement {
    private Map<String, ColorWrapper.RgbColor> colorStyles;
    private Map<String, Double> contrast;
    private Map<String, String> nonColorStyles;
    private String fragment;
}
