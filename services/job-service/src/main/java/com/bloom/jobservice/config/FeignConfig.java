package com.bloom.jobservice.config;

import com.bloom.jobservice.exception.JobsApiException;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableFeignClients(basePackages = "com.bloom.jobservice.external")
public class FeignConfig {

    @Bean
    @Profile("!native")
    public Logger.Level feignLoggerLevelProd() {
        return Logger.Level.NONE;
    }

    @Bean
    @Profile("native")
    public Logger.Level feignLoggerLevelDev() {
        return Logger.Level.BASIC;
    }

    @Bean
    public ErrorDecoder errorDecoder() {
        return (methodKey, response) -> {
            if (response.status() == 401) {
                return new JobsApiException("Invalid JobsAPI key");
            }
            if (response.status() == 429) {
                return new JobsApiException("JobsAPI quota exceeded");
            }
            return new JobsApiException("JobsAPI error: " + response.status());
        };
    }
}
