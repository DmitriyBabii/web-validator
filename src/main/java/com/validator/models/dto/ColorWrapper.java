package com.validator.models.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

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
        private static final int MAX_VALUE = 255;

        private int red;
        private int green;
        private int blue;

        private double alpha = MAX_VALUE;

        public int getRed() {
            double a = alpha / MAX_VALUE;
            return (int) Math.round(red * a + MAX_VALUE * (1 - a));
        }

        public int getGreen() {
            double a = alpha / MAX_VALUE;
            return (int) Math.round(green * a + MAX_VALUE * (1 - a));
        }

        public int getBlue() {
            double a = alpha / MAX_VALUE;
            return (int) Math.round(blue * a + MAX_VALUE * (1 - a));
        }


    }
}