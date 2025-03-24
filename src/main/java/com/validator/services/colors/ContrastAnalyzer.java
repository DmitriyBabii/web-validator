package com.validator.services.colors;

import com.validator.configs.selenium.SeleniumConnection;
import com.validator.models.dto.ColorAnalyzeElement;
import com.validator.models.dto.ColorAnalyzeReport;
import com.validator.models.dto.ColorWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.*;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContrastAnalyzer {

    public static final int MAX_COUNT_OF_CHILDREN = 5;

    private static final String CSS_BACKGROUND_PARAMETER = "background-color";

    // TODO replace empty with custom getText validation
    private static final String CSS_SELECTOR = String.format(
            ":has(> h1 > h2, > h3, > p, > ul, > ol, > span, > a):not(:has(:empty), :has(:nth-child(%d)))",
            MAX_COUNT_OF_CHILDREN);

    private static final List<String> CSS_COLOR_PARAMETERS = Arrays.asList(
            "color", "border-color", "border-top-color", "border-right-color",
            "border-bottom-color", "border-left-color", "outline-color",
            "outline", "background", "border-top-width", "border-left-width", "border-right-width",
            "border-bottom-width", CSS_BACKGROUND_PARAMETER
    );

    private static final String GET_STYLES_JS_SCRIPT = String.format("""
                    var element = arguments[0];
                    var styles = window.getComputedStyle(element);
                    var styleMap = {};
                    var relevantStyles = %s;
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
                    """,
            getCssParametersForScript()
    );

    private final SeleniumConnection seleniumConnection;
    private final ColorService colorService;

    public ColorAnalyzeReport analyzeByUrl(String url) {
        WebDriver driver = new RemoteWebDriver(seleniumConnection.driverURL(), seleniumConnection.chromeOptions());
        ColorAnalyzeReport colorAnalyzeReport = new ColorAnalyzeReport();
        Set<Map<String, String>> uniqueStyles = new HashSet<>();

        try {
            driver.get(url);
            List<WebElement> elements = driver.findElements(By.cssSelector(CSS_SELECTOR));
            log.info("Started to analyze {}", url);

            for (WebElement element : elements) {
                Map<String, String> styles = getComputedStyles(driver, element);

                if (styles.isEmpty() || !uniqueStyles.add(styles)) {
                    continue;
                }

                getOuterHTML(element).ifPresent(outerHtml -> {
                    Map<String, ColorWrapper.RgbColor> convertedColorStyles = convertColorStyles(styles);
                    ColorWrapper.RgbColor backgroundColor = convertedColorStyles.getOrDefault(CSS_BACKGROUND_PARAMETER, ColorWrapper.RgbColor.DEFAULT);

                    if (!isValidBackground(backgroundColor)) return;

                    Map<String, String> nonColorStyles = getNonColorStyles(styles, convertedColorStyles);
                    Map<String, Double> contrast = evaluateContrast(convertedColorStyles, backgroundColor);

                    colorAnalyzeReport.getElements().add(
                            ColorAnalyzeElement.builder()
                                    .colorStyles(convertedColorStyles)
                                    .nonColorStyles(nonColorStyles)
                                    .contrast(contrast)
                                    .fragment(outerHtml)
                                    .build()
                    );
                });
            }

            log.info("{} blocks were found and processed by {}", colorAnalyzeReport.getElements().size(), url);
            return colorAnalyzeReport;
        } finally {
            driver.quit();
        }
    }

    private static boolean isValidBackground(ColorWrapper.RgbColor background) {
        return !background.equals(ColorWrapper.RgbColor.DEFAULT);
    }

    private static Optional<String> getOuterHTML(WebElement element) {
        try {
            return Optional.ofNullable(element.getAttribute("outerHTML"));
        } catch (StaleElementReferenceException e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> getNonColorStyles(Map<String, String> styles, Map<String, ColorWrapper.RgbColor> convertedColorStyles) {
        return styles.entrySet().stream()
                .filter(entry -> convertedColorStyles.get(entry.getKey()) == null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
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
                            colorService.convertToRgbColor(Color.fromString(entry.getValue()))
                    )
            );
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private static Map<String, String> getComputedStyles(WebDriver driver, WebElement element) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            @SuppressWarnings("unchecked")
            Map<String, String> styles = (Map<String, String>) js.executeScript(GET_STYLES_JS_SCRIPT, element);

            return Optional.ofNullable(styles)
                    .orElse(Collections.emptyMap())
                    .entrySet()
                    .stream()
                    .filter(entry -> CSS_COLOR_PARAMETERS.contains(entry.getKey()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } catch (Exception e) {
            return Collections.emptyMap();
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


    private static List<String> getCssParametersForScript() {
        return CSS_COLOR_PARAMETERS.stream()
                .map(str -> '"' + str + '"')
                .toList();
    }
}