package com.bloom.jobservice.config;

import com.bloom.jobservice.dto.JobResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.util.List;

@Configuration
public class RedisConfig {

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
