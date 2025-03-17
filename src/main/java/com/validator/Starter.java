package com.validator;

import com.validator.services.ContrastAnalyzer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class Starter implements CommandLineRunner {
    private final ContrastAnalyzer contrastAnalyzer;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("<div class=\"FormControl-inlineValidation\" id=\"validation-eec4b917-07a6-46b5-b552-6ecd18c501c5\" hidden=\"hidden\">\n        <span class=\"FormControl-inlineValidation--visual\">\n          <svg aria-hidden=\"true\" height=\"12\" viewBox=\"0 0 12 12\" version=\"1.1\" width=\"12\" data-view-component=\"true\" class=\"octicon octicon-alert-fill\">\n    <path d=\"M4.855.708c.5-.896 1.79-.896 2.29 0l4.675 8.351a1.312 1.312 0 0 1-1.146 1.954H1.33A1.313 1.313 0 0 1 .183 9.058ZM7 7V3H5v4Zm-1 3a1 1 0 1 0 0-2 1 1 0 0 0 0 2Z\"></path>\n</svg>\n        </span>\n        <span></span>\n</div>");
    }

}
