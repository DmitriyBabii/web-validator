package com.validator.configs.selenium;

import org.openqa.selenium.chrome.ChromeOptions;

import java.net.URL;

public record SeleniumConnection(ChromeOptions chromeOptions, URL driverURL) {
}
