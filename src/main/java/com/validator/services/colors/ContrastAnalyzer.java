package com.validator.services.colors;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.validator.models.dto.ColorAnalyzeElement;
import com.validator.models.dto.ColorAnalyzeReport;
import com.validator.models.dto.ColorWrapper;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ContrastAnalyzer {

    public static final int MAX_COUNT_OF_CHILDREN = 5;
    private static final String CSS_BACKGROUND_PARAMETER = "background-color";
    private static final String CSS_SELECTOR = "body *:not(script):not(style):not(link):has(> h1, > h2, > h3, > p, > ul, > ol, > span, > a)";
    private static final List<String> CSS_COLOR_PARAMETERS = Arrays.asList(
            "color", "border-color", "border-top-color", "border-right-color",
            "border-bottom-color", "border-left-color", "box-shadow", "text-shadow", "outline-color",
            "outline", "background", "border-top-width", "border-left-width", "border-right-width",
            "border-bottom-width", CSS_BACKGROUND_PARAMETER
    );
    private static final String GET_STYLES_JS_SCRIPT = """
            var element = arguments[0];
            var styles = window.getComputedStyle(element);
            var styleMap = {};
            var relevantStyles = [
                "color", "border-top-color", "border-right-color",
                "border-bottom-color", "border-left-color",
                "outline-color", "background", "background-color",
                "border-top-width", "border-left-width", "border-right-width",
                "border-bottom-width"
            ];
            for (var i = 0; i < styles.length; i++) {
                var key = styles[i];
                if (relevantStyles.includes(key)) {
                    var value = styles.getPropertyValue(key);
                    if (value !== '' && value !== null) {
                        styleMap[key] = value;
                    }
                }
            }
            return styleMap;
            """;

    private final ChromeOptions chromeOptions;
    private final ColorService colorService;
    private final URL driverUrl;

    public ContrastAnalyzer(ChromeOptions chromeOptions, ColorService colorService, @Qualifier("driverUrl") URL driverUrl) {
        this.chromeOptions = chromeOptions;
        this.colorService = colorService;
        this.driverUrl = driverUrl;
    }

    public ColorAnalyzeReport analyzeByUrl(String url) {
        WebDriver driver = new RemoteWebDriver(driverUrl, chromeOptions);
        try {
            driver.get(url);

            List<WebElement> elements = driver.findElements(By.cssSelector(CSS_SELECTOR));
            Set<Map<String, String>> uniqueStyles = new HashSet<>();

            ColorAnalyzeReport colorAnalyzeReport = new ColorAnalyzeReport();
            log.info("Started to analyze {}", url);

            for (WebElement element : elements) {
                if (isInvalid(element)) continue;

                Map<String, String> styles = getComputedStyles(driver, element);

                if (!uniqueStyles.contains(styles)) {
                    uniqueStyles.add(styles);
                    String outerHTML = element.getAttribute("outerHTML");

                    Map<String, ColorWrapper.RgbColor> convertedColorStyles = convertColorStyles(styles);
                    Map<String, String> otherStyles = getOtherStyles(styles, convertedColorStyles);
                    Map<String, Double> contrast = evaluateContrast(convertedColorStyles, convertedColorStyles.get(CSS_BACKGROUND_PARAMETER));

                    colorAnalyzeReport.getElements().add(
                            ColorAnalyzeElement.builder()
                                    .styles(convertedColorStyles)
                                    .contrast(contrast)
                                    .fragment(outerHTML)
                                    .other(otherStyles)
                                    .build()
                    );
                }
            }

            log.info("{} blocks was found and processed by {}", uniqueStyles.size(), url);
            return colorAnalyzeReport;
        } finally {
            driver.close();
            driver.quit();
        }
    }

    private static Map<String, String> getOtherStyles(Map<String, String> styles, Map<String, ColorWrapper.RgbColor> convertedColorStyles) {
        return styles.entrySet().stream()
                .filter(entry -> convertedColorStyles.get(entry.getKey()) == null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private boolean isInvalid(WebElement element) {
        try {
            if (element.getText().isBlank() || hasChildrenMore(element)) {
                throw new IllegalArgumentException("Element has no text or has too many children");
            }
        } catch (StaleElementReferenceException | IllegalArgumentException e) {
            return true;
        }
        return false;
    }

    private Map<String, ColorWrapper.RgbColor> convertColorStyles(Map<String, String> styles) {
        return styles.entrySet()
                .stream()
                .map(this::convertEntryValueToRgbColor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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

    private double calculateContrastRatio(ColorWrapper.RgbColor color1, ColorWrapper.RgbColor color2) {
        double luminance1 = colorService.calculateLuminance(color1.getRed(), color1.getGreen(), color1.getBlue());
        double luminance2 = colorService.calculateLuminance(color2.getRed(), color2.getGreen(), color2.getBlue());

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