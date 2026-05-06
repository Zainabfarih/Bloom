package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.JobResponse;
import com.bloom.jobservice.dto.JobResult;
import com.bloom.jobservice.dto.JobSearchResponse;

import com.bloom.jobservice.exception.JobsApiException;
import com.bloom.jobservice.external.JobsApiClient;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobSearchService {

    private final JobsApiClient              jobsApiClient;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${jobapi.key}")
    private String jobsApiKey;

    private static final String   CACHE_PREFIX = "jobs:search:";
    private static final Duration CACHE_TTL    = Duration.ofHours(24);

    public JobSearchResponse searchJobs(String query, String location) {
        String cacheKey = buildCacheKey(query, location);

        // 1. Cache-Aside — vérifie Redis
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached instanceof List<?> cachedList) {
            log.debug("Cache HIT — key={}", cacheKey);
            return JobSearchResponse.builder()
                    .jobs((List<JobResult>) cachedList)
                    .fromCache(true)
                    .totalResults(cachedList.size())
                    .build();
        }

        log.debug("Cache MISS — calling JobsAPI");

        // 2. Appel JobsAPI
        try {
            JobResponse response = JobsApiClient.searchJobs(
                    "google_jobs",
                    query,
                    location != null ? location : "",
                    jobsApiKey
            );

            if (response.getError() != null) {
                throw new JobsApiException(
                        "JobsAPI error: " + response.getError());
            }

            List<JobResult> results = response.getJobsResults() != null
                    ? response.getJobsResults()
                    : List.of();

            // 3. Stocker dans Redis (24h)
            redisTemplate.opsForValue().set(cacheKey, results, CACHE_TTL);
            trackDailyApiCall();

            return JobSearchResponse.builder()
                    .jobs(results)
                    .fromCache(false)
                    .totalResults(results.size())
                    .build();

        } catch (FeignException e) {
            throw new JobsApiException(
                    "JobsAPI call failed — HTTP " + e.status(), e);
        }
    }

    public void evictCache(String query, String location) {
        String key = buildCacheKey(query, location);
        redisTemplate.delete(key);
        log.info("Cache evicted — key={}", key);
    }

    private String buildCacheKey(String query, String location) {
        String raw = (query + "|" + (location != null ? location : ""))
                .toLowerCase()
                .trim();
        return CACHE_PREFIX
                + DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void trackDailyApiCall() {
        String key = "jobs:calls:today";
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofDays(1));
        }
        if (count != null && count > 80) {
            log.warn("JobsAPI quota approaching — {} calls today", count);
        }
    }
}