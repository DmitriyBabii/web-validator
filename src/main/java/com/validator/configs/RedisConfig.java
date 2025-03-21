package com.validator.configs;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {

    public static final String LUMINANCE_GROUP = "luminance";

}
