package com.bloom.jobservice.config;

import feign.Logger;
import feign.codec.ErrorDecoder;
import com.bloom.jobservice.exception.JobsApiException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FeignConfig {

    // NONE en prod — évite de logger les URLs et headers (qui contiennent l'API key !)
    @Bean
    @Profile("!dev")
    public Logger.Level feignLoggerLevelProd() {
        return Logger.Level.NONE;
    }

    // BASIC uniquement en dev — pour débugger
    @Bean
    @Profile("dev")
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