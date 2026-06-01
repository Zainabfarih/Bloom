package com.bloom.jobservice.config;

import com.bloom.jobservice.dto.JobResult;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        return mock(RedisConnectionFactory.class);
    }

    @Bean("jobsRedisTemplate")
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, List<JobResult>> jobsRedisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean("skillsRedisTemplate")
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, List<String>> skillsRedisTemplate() {
        return mock(RedisTemplate.class);
    }

    @Bean("genericRedisTemplate")
    @Primary
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> genericRedisTemplate() {
        return mock(RedisTemplate.class);
    }
}