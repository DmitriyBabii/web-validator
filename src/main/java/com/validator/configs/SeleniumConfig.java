package com.validator.configs;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SeleniumConfig {

    @Bean
    public ChromeOptions chromeOptions() {
        return new ChromeOptions();
    }
}
