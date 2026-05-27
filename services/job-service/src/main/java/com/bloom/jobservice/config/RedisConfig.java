package com.bloom.jobservice.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.bloom.jobservice.dto.JobResult;
import io.lettuce.core.RedisURI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;

import java.time.Duration;
import java.util.List;

/**
 * RedisConfig — 3 templates Redis distincts :
 *
 *   1. jobsRedisTemplate    (List<JobResult>)  → cache des résultats de recherche SerpAPI
 *   2. skillsRedisTemplate  (List<String>)     → cache des skills extraits par Ollama
 *   3. genericRedisTemplate (Object/String)    → compteurs + index inversé jobId→searchKey
 *
 * Aucun activateDefaultTyping() → pas de @class dans le JSON stocké.
 * Compatible avec toutes les refactorisations de packages.
 */
@Configuration
public class RedisConfig {

    @Value("${spring.data.redis.url}")
    private String redisUrl;

    @Value("${spring.data.redis.timeout:2000ms}")
    private Duration commandTimeout;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
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

    /**
     * Template typé List<JobResult>.
     * Utilisé pour stocker les résultats de recherche complets (avec description).
     */
    @Bean("jobsRedisTemplate")
    public RedisTemplate<String, List<JobResult>> jobsRedisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = buildMapper();
        CollectionType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, JobResult.class);

        Jackson2JsonRedisSerializer<List<JobResult>> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, listType);

        RedisTemplate<String, List<JobResult>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Template typé List<String>.
     * Utilisé pour stocker les skills extraits par Ollama (cache lazy par jobId).
     */
    @Bean("skillsRedisTemplate")
    public RedisTemplate<String, List<String>> skillsRedisTemplate(RedisConnectionFactory factory) {
        ObjectMapper mapper = buildMapper();
        CollectionType listType = mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class);

        Jackson2JsonRedisSerializer<List<String>> serializer =
                new Jackson2JsonRedisSerializer<>(mapper, listType);

        RedisTemplate<String, List<String>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Template générique (String serializer) pour :
     *   - compteurs dailyCalls (Long sérialisé en String)
     *   - index inversé  jobs:jobid:{jobId} → searchCacheKey (String)
     */
    @Bean("genericRedisTemplate")
    public RedisTemplate<String, Object> genericRedisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(RedisSerializer.string());
        template.afterPropertiesSet();
        return template;
    }

    private ObjectMapper buildMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
