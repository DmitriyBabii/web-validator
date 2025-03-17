package com.validator.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.models.dto.ColorAnalyzeElement;
import com.validator.models.dto.ColorAnalyzeReport;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class ContrastAnalyzer {

    public static final int MAX_COUNT_OF_CHILDREN = 5;
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

    private static final Map<String, Double> illuminanceCache = new HashMap<>();

    private final ChromeOptions chromeOptions;
    private final URL driverUrl;

    public ContrastAnalyzer(ChromeOptions chromeOptions, @Qualifier("driverUrl") URL driverUrl) {
        this.chromeOptions = chromeOptions;
        this.driverUrl = driverUrl;
    }

    public ColorAnalyzeReport analyzeByUrl(String url) {
        WebDriver driver = new RemoteWebDriver(driverUrl, chromeOptions);
        try {
            driver.get(url);
            List<WebElement> elements = driver.findElements(By.cssSelector(CSS_SELECTOR));
            Set<Map<String, ColorWrapper.RgbColor>> uniqueStyles = new HashSet<>();
            log.info("Started to analyze {}", url);

            for (WebElement element : elements) {
                if (hasChildrenMore(element)) continue;
                Map<String, ColorWrapper.RgbColor> styles = getComputedStyles(driver, element)
                        .entrySet()
                        .stream()
                        .map(this::convertEntryValueToRgbColor)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                uniqueStyles.add(styles);
            }

            ColorAnalyzeReport colorAnalyzeReport = generateColorAnalyzeReport(uniqueStyles);
            log.info("{} blocks was found and processed by {}", uniqueStyles.size(), url);
            return colorAnalyzeReport;
        } finally {
            driver.close();
            driver.quit();
        }
    }

    private ColorAnalyzeReport generateColorAnalyzeReport(Set<Map<String, ColorWrapper.RgbColor>> uniqueStyles) {
        ColorAnalyzeReport colorAnalyzeReport = new ColorAnalyzeReport();

        uniqueStyles.forEach(styles -> {
            Map<String, Double> contrast = evaluateContrast(styles, styles.get(CSS_BACKGROUND_PARAMETER));
            colorAnalyzeReport.getElements().add(new ColorAnalyzeElement(styles, contrast));
        });

        return colorAnalyzeReport;
    }

    private Optional<Map.Entry<String, ColorWrapper.RgbColor>> convertEntryValueToRgbColor(Map.Entry<String, String> entry) {
        try {
            return Optional.of(
                    Map.entry(
                            entry.getKey(),
                            convertToRgbColor(Color.fromString(entry.getValue()))
                    )
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private boolean hasChildrenMore(WebElement element) {
        try {
            List<WebElement> children = element.findElements(By.cssSelector("*"));
            return children.size() > MAX_COUNT_OF_CHILDREN;
        } catch (Exception e) {
            return true;
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

    private double calculateLuminance(int r, int g, int b) {

        String key = getIlluminanceCacheKey(r, g, b);

        Double cachedValue = illuminanceCache.get(key);
        if (cachedValue != null) {
            return cachedValue;
        }

        double[] rgb = {r / 255.0, g / 255.0, b / 255.0};

        for (int i = 0; i < 3; i++) {
            if (rgb[i] <= 0.03928) {
                rgb[i] = rgb[i] / 12.92;
            } else {
                rgb[i] = Math.pow((rgb[i] + 0.055) / 1.055, 2.4);
            }
        }

        double value = 0.2126 * rgb[0] + 0.7152 * rgb[1] + 0.0722 * rgb[2];
        illuminanceCache.put(key, value);

        return value;
    }

    private String getIlluminanceCacheKey(int r, int g, int b) {
        return Stream.of(r, g, b)
                .map(String::valueOf)
                .collect(Collectors.joining(","));
    }

    private double calculateContrastRatio(ColorWrapper.RgbColor color1, ColorWrapper.RgbColor color2) {
        double luminance1 = calculateLuminance(color1.getRed(), color1.getGreen(), color1.getBlue());
        double luminance2 = calculateLuminance(color2.getRed(), color2.getGreen(), color2.getBlue());

        double max = Math.max(luminance1, luminance2);
        double min = Math.min(luminance1, luminance2);

        return (max + 0.05) / (min + 0.05);
    }

    private Map<String, Double> evaluateContrast(Map<String, ColorWrapper.RgbColor> colorStyles, ColorWrapper.RgbColor backgroundColor) {
        return colorStyles.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(CSS_BACKGROUND_PARAMETER))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> calculateContrastRatio(entry.getValue(), backgroundColor)
                ));
    }


    private ColorWrapper.RgbColor convertToRgbColor(Color color) {
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.convertValue(color, ColorWrapper.class).getColor();
    }
}