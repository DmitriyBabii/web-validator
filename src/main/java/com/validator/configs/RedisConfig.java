package com.validator.configs;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {

    public static final String LUMINANCE_GROUP = "luminance";
    public static final String COLOR_CONVERTER_GROUP = "color_convert";
//
//    private static final Duration TTL_DURATION = Duration.ofHours(1);
//
//    @Bean
//    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
//        return RedisCacheManager.builder(redisConnectionFactory)
//                .cacheDefaults(RedisCacheConfiguration.defaultCacheConfig()
//                        .entryTtl(TTL_DURATION))
//                .build();
//    }
}
