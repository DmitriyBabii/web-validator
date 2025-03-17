package com.validator.configs;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URL;

@Configuration
public class SeleniumConfig {
    private static final String DRIVER_URL = "http://localhost:4444/wd/hub";

    @Bean
    public ChromeOptions chromeOptions() {
        return new ChromeOptions();
    }

    @Bean("driverUrl")
    public URL driverUrl() throws MalformedURLException {
        return new URL(DRIVER_URL);
    }
}
