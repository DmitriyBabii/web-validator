package com.validator.services.colors;

import com.validator.configs.RedisConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class ColorService {

    @Cacheable(value = RedisConfig.LUMINANCE_GROUP, key = "#r + '_' + #g + '_' + #b")
    public double calculateLuminance(int r, int g, int b) {
        double[] rgb = {r / 255.0, g / 255.0, b / 255.0};

        for (int i = 0; i < 3; i++) {
            if (rgb[i] <= 0.03928) {
                rgb[i] = rgb[i] / 12.92;
            } else {
                rgb[i] = Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
            }
        }

        return 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
    }
}
