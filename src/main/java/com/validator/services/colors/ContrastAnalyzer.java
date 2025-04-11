package com.validator.services.colors;

import com.validator.configs.selenium.SeleniumConnection;
import com.validator.models.dto.ColorAnalyzeElement;
import com.validator.models.dto.ColorAnalyzeReport;
import com.validator.models.dto.ColorWrapper;
import com.validator.models.dto.ElementStyle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.Color;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContrastAnalyzer {

    public static final int MAX_COUNT_OF_CHILDREN = 3;

    private static final String CSS_BACKGROUND_PARAMETER = "background-color";

    private static final String CSS_SELECTOR = String.format(
            ":has(> h1 > h2, > h3, > p, > ul, > ol, > span, > a):not(:has(:empty), :has(:nth-child(%d)))",
            MAX_COUNT_OF_CHILDREN);

    private static final List<String> CSS_COLOR_PARAMETERS = Arrays.asList(
            "color", "border-color", "border-top-color", "border-right-color",
            "border-bottom-color", "border-left-color", "outline-color",
            "outline", "background", "border-top-width", "border-left-width", "border-right-width",
            "border-bottom-width", CSS_BACKGROUND_PARAMETER
    );

    private static final String GET_STYLES_JS_SCRIPT = """
            let elements = document.querySelectorAll(arguments[0]);
            let relevantStyles = arguments[1];
            let results = [];
                        
            for (let i = 0; i < elements.length; i++) {
                let el = elements[i];
                let styles = window.getComputedStyle(el);
                let styleMap = {};
                let hasAny = false;
                                
                for (let j = 0; j < relevantStyles.length; j++) {
                    let key = relevantStyles[j];
                    let val = styles.getPropertyValue(key);
                    if (val && val.trim() !== '') {
                        styleMap[key] = val;
                        hasAny = true;
                    }
                }
                                
                if (hasAny) {
                    results.push({
                        outerHTML: el.outerHTML,
                        styles: styleMap
                    });
                }
            }
            return results;
            """;

    private final SeleniumConnection seleniumConnection;
    private final ColorService colorService;

    public synchronized ColorAnalyzeReport analyzeByUrl(String url) {
        WebDriver driver = new RemoteWebDriver(seleniumConnection.driverURL(), seleniumConnection.chromeOptions());
        ColorAnalyzeReport colorAnalyzeReport = new ColorAnalyzeReport();
        Set<Map<String, String>> uniqueStyles = new HashSet<>();

        try {
            driver.get(url);

            log.info("Started to analyze {}", url);
            List<ElementStyle> elements = getElementStyles(driver);

            for (ElementStyle element : elements) {
                Map<String, String> styles = element.getStyles();
                if (!uniqueStyles.add(styles)) {
                    continue;
                }

                Map<String, ColorWrapper.RgbColor> convertedColorStyles = convertColorStyles(styles);
                ColorWrapper.RgbColor backgroundColor = convertedColorStyles.getOrDefault(CSS_BACKGROUND_PARAMETER, ColorWrapper.RgbColor.DEFAULT);

                Map<String, String> nonColorStyles = getNonColorStyles(styles, convertedColorStyles);
                Map<String, Double> contrast = evaluateContrast(convertedColorStyles, backgroundColor);

                colorAnalyzeReport.getElements().add(
                        ColorAnalyzeElement.builder()
                                .colorStyles(convertedColorStyles)
                                .nonColorStyles(nonColorStyles)
                                .contrast(contrast)
                                .fragment(element.getOuterHtml())
                                .build()
                );
            }

            log.info("{} blocks were found and processed by {}", colorAnalyzeReport.getElements().size(), url);
            return colorAnalyzeReport;
        } finally {
            driver.quit();
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

    private static List<ElementStyle> getElementStyles(WebDriver driver) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawStyles = (List<Map<String, Object>>) Objects.requireNonNull(js.executeScript(
                    GET_STYLES_JS_SCRIPT,
                    CSS_SELECTOR,
                    CSS_COLOR_PARAMETERS
            ));

            return rawStyles.stream()
                    .map(el -> {
                        @SuppressWarnings("unchecked")
                        Map<String, String> styles = (Map<String, String>) el.get("styles");
                        return ElementStyle.builder()
                                .outerHtml((String) el.get("outerHTML"))
                                .styles(styles)
                                .build();
                    })
                    .toList();

        } catch (Exception e) {
            log.warn("Cant get styles: {}", e.getMessage());
            return List.of();
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
}