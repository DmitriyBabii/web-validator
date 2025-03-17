package com.validator.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.models.dto.ColorWrapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContrastAnalyzer {

    public static final int MAX_COUNT_OF_CHILDREN = 10;
    private static final String CSS_BACKGROUND_PARAMETER = "background-color";
    private static final String CSS_SELECTOR = "body *:not(script):not(style):not(link):has(> h1, > h2, > h3, > p, > ul, > ol, > span, > a)";
    private static final List<String> CSS_COLOR_PARAMETERS = Arrays.asList(
            "color", "border-color", "border-top-color", "border-right-color",
            "border-bottom-color", "border-left-color", "box-shadow", "text-shadow", "outline-color",
            "outline", CSS_BACKGROUND_PARAMETER
    );
    private static final String GET_STYLES_JS_SCRIPT = """
            var element = arguments[0];
            var styles = window.getComputedStyle(element);
            var styleMap = {};
            for (var i = 0; i < styles.length; i++) {
                styleMap[styles[i]] = styles.getPropertyValue(styles[i]);
            }
            return styleMap;
            """;

    private final ChromeOptions chromeOptions;
    private final URL driverUrl;

    public ContrastAnalyzer(ChromeOptions chromeOptions, @Qualifier("driverUrl") URL driverUrl) {
        this.chromeOptions = chromeOptions;
        this.driverUrl = driverUrl;
    }

    public void analyzeByUrl(String url) {
        WebDriver driver = new RemoteWebDriver(driverUrl, chromeOptions);
        try {
            driver.get(url);
            List<WebElement> elements = driver.findElements(By.cssSelector(CSS_SELECTOR));

            int count = 0;

            for (WebElement element : elements) {
                try {
                    List<WebElement> children = element.findElements(By.cssSelector("*"));
                    if (children.size() > MAX_COUNT_OF_CHILDREN) {
                        continue;
                    }
                } catch (Exception e) {
                    continue;
                }

                // TODO simplify to one Map using stream
                Map<String, ColorWrapper.RgbColor> convertedStyles = new HashMap<>();
                Map<String, String> styles = getComputedStyles(driver, element);
                styles.forEach((key, value) -> {
                            try {
                                convertedStyles.put(key, convertToRgbColor(Color.fromString(value)));
                            } catch (Exception e) {

                            }
                        }
                );
                Map<String, Double> contrast = evaluateContrast(convertedStyles, convertedStyles.get(CSS_BACKGROUND_PARAMETER));
                if (contrast.values().stream().allMatch(value -> value == 1.0)) {
                    continue;
                }
                System.out.println(convertedStyles);
                System.out.println(contrast);
                count++;
            }

            System.out.println(count);
        } finally {
            driver.close();
            driver.quit();
        }
    }

    private static Map<String, String> getComputedStyles(WebDriver driver, WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;
            Map<String, String> styles = (Map<String, String>) js.executeScript(GET_STYLES_JS_SCRIPT, element);

            return styles != null ?
                    styles.entrySet().stream()
                            .filter(entry -> CSS_COLOR_PARAMETERS.contains(entry.getKey()))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue))
                    : new HashMap<>();
        } catch (Exception e) {
            return new HashMap<>();
        }
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

// TODO make cache for lumis(global) and for values(local) to make them less