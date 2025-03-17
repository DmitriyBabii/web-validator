package com.validator.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.models.dto.ColorWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContrastAnalyzer {

    // TODO remove with parameter
    public static final String URL = "https://comfy.ua/ua/irpin/";
    public static final String DRIVER_URL = "http://localhost:4444/wd/hub";

    public static final String CSS_BACKGROUND_PARAMETER = "background-color";
    public static final List<String> CSS_COLOR_PARAMETERS = Arrays.asList(
            "color", "background-color", "border-color", "border-top-color", "border-right-color",
            "border-bottom-color", "border-left-color", "box-shadow", "text-shadow", "outline-color",
            "outline"
    );
    public static final String SCRIPT = """
            var element = arguments[0];
            var styles = window.getComputedStyle(element);
            var styleMap = {};
            for (var i = 0; i < styles.length; i++) {
                styleMap[styles[i]] = styles.getPropertyValue(styles[i]);
            }
            return styleMap;
            """;

    private final ChromeOptions chromeOptions;

    public void analyzeByUrl() throws MalformedURLException {
        WebDriver driver = new RemoteWebDriver(new URL(DRIVER_URL), chromeOptions);
        try {
            driver.get(URL);
            WebElement element = driver.findElement(By.cssSelector("div.header-bottom button"));

            Map<String, ColorWrapper.RgbColor> convertedStyles = new HashMap<>();

            Map<String, String> styles = getComputedStyles(driver, element);
            styles.forEach((key, value) -> {
                        try {
                            convertedStyles.put(key, convertToRgbColor(Color.fromString(value)));
                        } catch (Exception ex) {
                            log.info("Cant convert {}", value);
                        }
                    }
            );

            Map<String, Double> contrast = evaluateContrast(convertedStyles, convertedStyles.get(CSS_BACKGROUND_PARAMETER));

            System.out.println(convertedStyles);
            System.out.println(contrast);

        } finally {
            driver.close();
            driver.quit();
        }
    }

    private static Map<String, String> getComputedStyles(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        Map<String, String> styles = (Map<String, String>) js.executeScript(SCRIPT, element);

        return styles != null ?
                styles.entrySet().stream()
                        .filter(entry -> CSS_COLOR_PARAMETERS.contains(entry.getKey()))
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                : new HashMap<>();
    }

    private static double calculateLuminance(int r, int g, int b) {
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


    private static double calculateContrastRatio(ColorWrapper.RgbColor color1, ColorWrapper.RgbColor color2) {
        double luminance1 = calculateLuminance(color1.getRed(), color1.getGreen(), color1.getBlue());
        double luminance2 = calculateLuminance(color2.getRed(), color2.getGreen(), color2.getBlue());

        double L1 = Math.max(luminance1, luminance2);
        double L2 = Math.min(luminance1, luminance2);

        return (L1 + 0.05) / (L2 + 0.05);
    }

    private static Map<String, Double> evaluateContrast(Map<String, ColorWrapper.RgbColor> colorStyles, ColorWrapper.RgbColor backgroundColor) {
        return colorStyles.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(CSS_BACKGROUND_PARAMETER))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateContrastRatio(entry.getValue(), backgroundColor)
                ));
    }


    private static ColorWrapper.RgbColor convertToRgbColor(Color color) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(color, ColorWrapper.class).getColor();
    }
}