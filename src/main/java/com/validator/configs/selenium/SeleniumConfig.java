package com.validator.configs.selenium;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Configuration
public class SeleniumConfig {
    private static final String DRIVER_URL = "http://localhost:4444/wd/hub";

    @Bean
    public SeleniumConnection seleniumConnection() throws MalformedURLException {
        return new SeleniumConnection(chromeOptions(), driverUrl());
    }


    private ChromeOptions chromeOptions() {
        return new ChromeOptions();
    }

    private URL driverUrl() throws MalformedURLException {
        return URL.of(URI.create(DRIVER_URL), null);
    }
}
