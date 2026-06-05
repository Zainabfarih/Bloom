package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.*;
import com.bloom.jobservice.exception.JobsApiException;
import com.bloom.jobservice.exception.ResourceNotFoundException;
import com.bloom.jobservice.external.JobsApiClient;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class JobService {

    private final RedisTemplate<String, List<JobResult>> jobsRedisTemplate;
    private final RedisTemplate<String, List<String>>    skillsRedisTemplate;
    private final RedisTemplate<String, Object>          genericRedisTemplate;
    private final JobsApiClient     jobsApiClient;
    private final JobSkillExtractor skillExtractor;

    public JobService(
            @Qualifier("jobsRedisTemplate")    RedisTemplate<String, List<JobResult>> jobsRedisTemplate,
            @Qualifier("skillsRedisTemplate")  RedisTemplate<String, List<String>>    skillsRedisTemplate,
            @Qualifier("genericRedisTemplate") RedisTemplate<String, Object>          genericRedisTemplate,
            JobsApiClient jobsApiClient,
            JobSkillExtractor skillExtractor) {
        this.jobsRedisTemplate    = jobsRedisTemplate;
        this.skillsRedisTemplate  = skillsRedisTemplate;
        this.genericRedisTemplate = genericRedisTemplate;
        this.jobsApiClient        = jobsApiClient;
        this.skillExtractor       = skillExtractor;
    }

    @Value("${jobsapi.key}")
    private String jobsApiKey;

    private static final String   SEARCH_PREFIX = "jobs:search:";
    private static final String   JOBID_INDEX   = "jobs:jobid:";
    private static final String   DETAIL_PREFIX = "jobs:detail:";
    private static final Duration CACHE_TTL     = Duration.ofHours(24);
    private static final int      QUOTA_WARN    = 80;

    public JobSearchResponse searchJobs(String query, String location) {
        String searchKey = buildSearchKey(query, location);

        List<JobResult> cached = safeGetJobList(searchKey);
        if (cached != null) {
            log.debug("Search cache HIT — key={}", searchKey);
            return buildSearchResponse(cached, true);
        }

        log.debug("Search cache MISS — calling SerpAPI");
        try {
            JobResponse response = jobsApiClient.searchJobs(
                    "google_jobs",
                    query,
                    location != null ? location : "",
                    jobsApiKey
            );

            // CORRECTION ICI : Gestion gracieuse de l'absence de résultats
            if (response.getError() != null) {
                if (response.getError().contains("Google hasn't returned any results")) {
                    log.info("No jobs found for query: '{}' in location: '{}'. Returning empty list.", query, location);
                    // On met en cache la liste vide pour éviter de respammer l'API pour une recherche vide
                    jobsRedisTemplate.opsForValue().set(searchKey, Collections.emptyList(), CACHE_TTL);
                    return buildSearchResponse(Collections.emptyList(), false);
                }
                // Si c'est une vraie erreur (clé invalide, quota dépassé, etc.), on lance l'exception
                throw new JobsApiException("JobsAPI error: " + response.getError());
            }

            List<JobResult> results = response.getJobsResults() != null
                    ? response.getJobsResults()
                    : List.of();

            jobsRedisTemplate.opsForValue().set(searchKey, results, CACHE_TTL);

            results.forEach(job -> {
                if (job.getJobId() != null) {
                    genericRedisTemplate.opsForValue()
                            .set(JOBID_INDEX + job.getJobId(), searchKey, CACHE_TTL);
                }
            });

            trackDailyApiCall();
            return buildSearchResponse(results, false);

        } catch (FeignException e) {
            // Sécurité supplémentaire au cas où l'API renvoie un vrai 404 HTTP
            if (e.status() == 404) {
                log.info("API returned 404 Not Found for query: '{}'. Returning empty list.", query);
                return buildSearchResponse(Collections.emptyList(), false);
            }
            throw new JobsApiException("JobsAPI call failed — HTTP " + e.status(), e);
        }
    }

    public JobDetailResponse getJobDetail(String jobId) {
        JobResult job = findJobInCache(jobId);
        if (job == null) {
            throw new ResourceNotFoundException(
                    "Job not found in cache. Run a search containing this job first. jobId=" + jobId);
        }

        String skillKey = DETAIL_PREFIX + jobId;
        List<String> cachedSkills = safeGetSkillList(skillKey);

        if (cachedSkills != null) {
            log.debug("Skills cache HIT — jobId={}", jobId);
            return toDetailResponse(job, cachedSkills, true);
        }

        log.debug("Skills cache MISS — extracting with Ollama for jobId={}", jobId);
        List<String> skills = skillExtractor.extract(job.getDescription());
        skillsRedisTemplate.opsForValue().set(skillKey, skills, CACHE_TTL);
        log.info("Skills extracted and cached — jobId={}, count={}", jobId, skills.size());

        return toDetailResponse(job, skills, false);
    }

    public void evictCache(String query, String location) {
        String searchKey = buildSearchKey(query, location);
        List<JobResult> jobs = safeGetJobList(searchKey);
        if (jobs != null) {
            jobs.forEach(job -> {
                if (job.getJobId() != null) {
                    genericRedisTemplate.delete(JOBID_INDEX + job.getJobId());
                    skillsRedisTemplate.delete(DETAIL_PREFIX + job.getJobId());
                }
            });
        }
        jobsRedisTemplate.delete(searchKey);
        log.info("Cache evicted — searchKey={}", searchKey);
    }

    private JobResult findJobInCache(String jobId) {
        try {
            Object searchKeyObj = genericRedisTemplate.opsForValue().get(JOBID_INDEX + jobId);
            if (searchKeyObj == null) return null;

            List<JobResult> jobs = jobsRedisTemplate.opsForValue().get(searchKeyObj.toString());
            if (jobs == null) return null;

            return jobs.stream()
                    .filter(j -> jobId.equals(j.getJobId()))
                    .findFirst()
                    .orElse(null);

        } catch (Exception e) {
            log.warn("Index lookup failed for jobId={}: {}", jobId, e.getMessage());
            return null;
        }
    }

    private JobSearchResponse buildSearchResponse(List<JobResult> results, boolean fromCache) {
        return JobSearchResponse.builder()
                .jobs(results.stream().map(JobSearchResult::from).toList())
                .fromCache(fromCache)
                .totalResults(results.size())
                .build();
    }

    private JobDetailResponse toDetailResponse(JobResult job, List<String> skills, boolean fromSkillCache) {
        return JobDetailResponse.builder()
                .jobId(job.getJobId())
                .title(job.getTitle())
                .companyName(job.getCompanyName())
                .location(job.getLocation())
                .description(job.getDescription())
                .extensions(job.getExtensions())
                .applyOptions(job.getApplyOptions())
                .extractedSkills(skills)
                .fromSkillCache(fromSkillCache)
                .fromSearchCache(true)
                .build();
    }

    private List<JobResult> safeGetJobList(String key) {
        try {
            List<JobResult> v = jobsRedisTemplate.opsForValue().get(key);
            return (v != null && !v.isEmpty()) ? v : null;
        } catch (Exception e) {
            log.warn("Cache read failed key={} — evicting: {}", key, e.getMessage());
            jobsRedisTemplate.delete(key);
            return null;
        }
    }

    private List<String> safeGetSkillList(String key) {
        try {
            return skillsRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Skill cache read failed key={} — evicting: {}", key, e.getMessage());
            skillsRedisTemplate.delete(key);
            return null;
        }
    }

    private String buildSearchKey(String query, String location) {
        String raw = (query + "|" + (location != null ? location : "")).toLowerCase().trim();
        return SEARCH_PREFIX + DigestUtils.md5DigestAsHex(raw.getBytes(StandardCharsets.UTF_8));
    }

    private void trackDailyApiCall() {
        String key = "jobs:calls:today";
        try {
            Long count = genericRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) genericRedisTemplate.expire(key, Duration.ofDays(1));
            if (count != null && count > QUOTA_WARN) log.warn("JobsAPI quota — {} calls today", count);
        } catch (Exception e) {
            log.warn("Could not track API call: {}", e.getMessage());
        }
    }
}