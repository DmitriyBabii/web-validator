package com.validator.models.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Map;

@Getter
@AllArgsConstructor
public class ColorAnalyzeElement {
    private Map<String, ColorWrapper.RgbColor> styles;
    private Map<String, Double> contrast;
    private String fragment;
}
