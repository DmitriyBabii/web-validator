package com.validator.models.dto;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class ColorAnalyzeReport {
    private List<ColorAnalyzeElement> elements = new ArrayList<>();
}
