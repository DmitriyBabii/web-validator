package com.validator.configs.selenium;

import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@Configuration
public class SeleniumConfig {
    @Value("${driver.url}")
    private String driverUrl;

    @Bean
    public SeleniumConnection seleniumConnection() throws MalformedURLException {
        return new SeleniumConnection(chromeOptions(), driverUrl());
    }

    private ChromeOptions chromeOptions() {
        return new ChromeOptions();
    }

    private URL driverUrl() throws MalformedURLException {
        return URL.of(URI.create(driverUrl), null);
    }
}
