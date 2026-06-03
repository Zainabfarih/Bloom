package com.bloom.roadmapservice.config;

import feign.Logger;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableFeignClients(basePackages = "com.bloom.roadmapservice.external")
@Slf4j
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
            // Lire le body AVANT de fermer la Response
            String bodyText = readBody(response);
            log.error("Feign error — method={} status={} body={}", methodKey, response.status(), bodyText);
            // Retourner une RuntimeException standard — pas de FeignException interne
            return new RuntimeException(
                    "job-service returned HTTP " + response.status() + " on " + methodKey
            );
        };
    }

    private String readBody(Response response) {
        try {
            if (response.body() != null) {
                return new String(response.body().asInputStream().readAllBytes());
            }
        } catch (Exception ignored) {}
        return "(empty body)";
    }
}