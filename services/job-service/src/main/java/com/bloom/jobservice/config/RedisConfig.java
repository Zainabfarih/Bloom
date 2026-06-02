package com.bloom.jobservice.config;

import com.bloom.jobservice.dto.JobResult;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.type.TypeFactory;

import java.util.List;

@Configuration
@EnableCaching
public class RedisConfig {

    private static final TypeFactory TYPE_FACTORY = TypeFactory.createDefaultInstance();

    /**
     * Serializer for {@code List<JobResult>}.
     * <p>
     * The element type MUST be carried via a Jackson {@link JavaType}. Building the serializer
     * from a raw {@code List.class} erases the element type, so on read Jackson rebuilds each
     * entry as a {@code LinkedHashMap} instead of a {@link JobResult}, which then triggers a
     * {@code ClassCastException} when the cached value is consumed.
     */
    @Bean("jobsRedisTemplate")
    @ConditionalOnMissingBean(name = "jobsRedisTemplate")
    public RedisTemplate<String, List<JobResult>> jobsRedisTemplate(RedisConnectionFactory factory) {
        JavaType type = TYPE_FACTORY.constructCollectionType(List.class, JobResult.class);
        JacksonJsonRedisSerializer<List<JobResult>> serializer =
                new JacksonJsonRedisSerializer<>(type);

        RedisTemplate<String, List<JobResult>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Serializer for {@code List<String>} (extracted skills cache).
     */
    @Bean("skillsRedisTemplate")
    @ConditionalOnMissingBean(name = "skillsRedisTemplate")
    public RedisTemplate<String, List<String>> skillsRedisTemplate(RedisConnectionFactory factory) {
        JavaType type = TYPE_FACTORY.constructCollectionType(List.class, String.class);
        JacksonJsonRedisSerializer<List<String>> serializer =
                new JacksonJsonRedisSerializer<>(type);

        RedisTemplate<String, List<String>> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(RedisSerializer.string());
        template.setValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * Generic String-based template used for the jobId index and the daily-call counter.
     */
    @Bean("genericRedisTemplate")
    @ConditionalOnMissingBean(name = "genericRedisTemplate")
    @SuppressWarnings("unchecked")
    public RedisTemplate<String, Object> genericRedisTemplate(RedisConnectionFactory factory) {
        StringRedisTemplate template = new StringRedisTemplate();
        template.setConnectionFactory(factory);
        template.afterPropertiesSet();
        return (RedisTemplate<String, Object>) (RedisTemplate<?, ?>) template;
    }
}
