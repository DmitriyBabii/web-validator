package com.validator.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ColorWrapper {
    private RgbColor color;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RgbColor {
        public static final RgbColor DEFAULT = new RgbColor(0, 0, 0);

        private int red;
        private int green;
        private int blue;
    }
}