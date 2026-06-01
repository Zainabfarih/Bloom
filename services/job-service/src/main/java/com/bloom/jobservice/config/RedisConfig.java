package com.bloom.jobservice.config;

import com.bloom.jobservice.dto.JobResult;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.List;

@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "redis.ssl.enabled", havingValue = "true")
    public RedisConnectionFactory redisConnectionFactory(
            @Value("${spring.data.redis.timeout}") Duration commandTimeout) {

        RedisURI uri = RedisURI.create(redisUrl);
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
                .commandTimeout(commandTimeout)
                .useSsl()
                .build();
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration(uri.getHost(), uri.getPort());
        standalone.setUsername(uri.getUsername());
        standalone.setPassword(new String(uri.getPassword()));
        return new LettuceConnectionFactory(standalone, clientConfig);
    }


    @Bean
    @ConditionalOnMissingBean(RedisConnectionFactory.class)
    @ConditionalOnProperty(name = "redis.ssl.enabled", havingValue = "false",
            matchIfMissing = true)
    public RedisConnectionFactory redisConnectionFactoryNoSsl() {
        RedisURI uri = RedisURI.create(redisUrl);
        RedisStandaloneConfiguration standalone =
                new RedisStandaloneConfiguration(uri.getHost(), uri.getPort());
        return new LettuceConnectionFactory(standalone,
                LettuceClientConfiguration.defaultConfiguration());
    }


    @SuppressWarnings("unchecked")
    @Bean("jobsRedisTemplate")
    @ConditionalOnMissingBean(name = "jobsRedisTemplate")
    public RedisTemplate<String, List<JobResult>> jobsRedisTemplate(RedisConnectionFactory factory) {
        JacksonJsonRedisSerializer<List> serializer = new JacksonJsonRedisSerializer<>(List.class);
        RedisTemplate<String, List<JobResult>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer((RedisSerializer) serializer);
        template.afterPropertiesSet();
        return template;
    }

    @SuppressWarnings("unchecked")
    @Bean("skillsRedisTemplate")
    @ConditionalOnMissingBean(name = "skillsRedisTemplate")
    public RedisTemplate<String, List<String>> skillsRedisTemplate(RedisConnectionFactory factory) {
        JacksonJsonRedisSerializer<List> serializer = new JacksonJsonRedisSerializer<>(List.class);
        RedisTemplate<String, List<String>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer((RedisSerializer) serializer);
        template.afterPropertiesSet();
        return template;
    }

    @SuppressWarnings("unchecked")
    @Bean("genericRedisTemplate")
    @ConditionalOnMissingBean(name = "genericRedisTemplate")
    public RedisTemplate<String, Object> genericRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return (RedisTemplate<String, Object>) (RedisTemplate<?, ?>) template;
    }
}