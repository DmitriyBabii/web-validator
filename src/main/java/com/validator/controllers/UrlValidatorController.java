package com.validator.controllers;

import com.validator.models.dto.ColorAnalyzeReport;
import com.validator.models.dto.TidyReport;
import com.validator.models.dto.UrlRequest;
import com.validator.services.ContrastAnalyzer;
import com.validator.services.TidyValidator;
import com.validator.services.UrlProcessor;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/validate")
@RequiredArgsConstructor
public class UrlValidatorController {
    private final UrlProcessor urlProcessor;
    private final TidyValidator tidyValidator;
    private final ContrastAnalyzer contrastAnalyzer;

    @PostMapping("/html")
    public ResponseEntity<TidyReport> htmlTest(@RequestBody @Valid UrlRequest url) {
        Optional<String> html = urlProcessor.getHtml(url.getUrl());
        return html.map(s -> ResponseEntity.ok(tidyValidator.validate(s)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // TODO make another request class
    @PostMapping("/color")
    public ResponseEntity<ColorAnalyzeReport> colorTest(@RequestBody @Valid UrlRequest url) {
        return ResponseEntity.ok(contrastAnalyzer.analyzeByUrl(url.getUrl()));
    }
}
