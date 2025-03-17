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
    }

}
