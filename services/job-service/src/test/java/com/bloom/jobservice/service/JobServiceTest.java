package com.bloom.jobservice.service;

import com.bloom.jobservice.dto.*;
import com.bloom.jobservice.exception.JobsApiException;
import com.bloom.jobservice.exception.ResourceNotFoundException;
import com.bloom.jobservice.external.JobsApiClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JobServiceTest {

    @Mock
    private RedisTemplate<String, List<JobResult>> jobsRedisTemplate;
    @Mock
    private RedisTemplate<String, List<String>> skillsRedisTemplate;
    @Mock
    private RedisTemplate<String, Object> genericRedisTemplate;
    @Mock
    private JobsApiClient jobsApiClient;
    @Mock
    private JobSkillExtractor skillExtractor;

    @Mock
    private ValueOperations<String, List<JobResult>> jobsOps;
    @Mock
    private ValueOperations<String, List<String>> skillsOps;
    @Mock
    private ValueOperations<String, Object> genericOps;

    private JobService jobService;

    @BeforeEach
    void setUp() {
        when(jobsRedisTemplate.opsForValue()).thenReturn(jobsOps);
        when(skillsRedisTemplate.opsForValue()).thenReturn(skillsOps);
        when(genericRedisTemplate.opsForValue()).thenReturn(genericOps);

        jobService = new JobService(
                jobsRedisTemplate, skillsRedisTemplate, genericRedisTemplate,
                jobsApiClient, skillExtractor);
    }

    // ─── searchJobs ──────────────────────────────────────────────────────────

    @Test
    void searchJobs_cache_hit_returns_response_without_skills() {
        List<JobResult> cached = List.of(buildJobResult("id-1", "Java Dev", "ALTEN"));
        when(jobsOps.get(anyString())).thenReturn(cached);

        JobSearchResponse result = jobService.searchJobs("java", "Morocco");

        assertThat(result.isFromCache()).isTrue();
        assertThat(result.getTotalResults()).isEqualTo(1);
        assertThat(result.getJobs()).hasSize(1);
        assertThat(result.getJobs().get(0).getJobId()).isEqualTo("id-1");
        verifyNoInteractions(jobsApiClient, skillExtractor);
    }

    @Test
    void searchJobs_cache_miss_calls_api_and_stores_results() {
        when(jobsOps.get(anyString())).thenReturn(null);

        List<JobResult> apiResults = List.of(
                buildJobResult("id-2", "Senior Java", "Arrow"),
                buildJobResult("id-3", "Fullstack Dev", "WLG")
        );
        JobResponse apiResponse = new JobResponse();
        apiResponse.setJobsResults(apiResults);
        when(jobsApiClient.searchJobs(any(), any(), any(), any())).thenReturn(apiResponse);
        when(genericOps.increment(anyString())).thenReturn(1L);

        JobSearchResponse result = jobService.searchJobs("java", "Casablanca");

        assertThat(result.isFromCache()).isFalse();
        assertThat(result.getTotalResults()).isEqualTo(2);
        verifyNoInteractions(skillExtractor);
        verify(jobsOps).set(anyString(), eq(apiResults), any());
        verify(genericOps, times(2)).set(startsWith("jobs:jobid:"), anyString(), any());
    }

    @Test
    void searchJobs_api_error_throws_JobsApiException() {
        when(jobsOps.get(anyString())).thenReturn(null);
        JobResponse errorResponse = new JobResponse();
        errorResponse.setError("quota exceeded");
        when(jobsApiClient.searchJobs(any(), any(), any(), any())).thenReturn(errorResponse);

        assertThatThrownBy(() -> jobService.searchJobs("java", "Morocco"))
                .isInstanceOf(JobsApiException.class)
                .hasMessageContaining("quota exceeded");
    }

    @Test
    void searchJobs_null_results_returns_empty_list() {
        when(jobsOps.get(anyString())).thenReturn(null);
        JobResponse emptyResponse = new JobResponse();
        emptyResponse.setJobsResults(null);
        when(jobsApiClient.searchJobs(any(), any(), any(), any())).thenReturn(emptyResponse);
        when(genericOps.increment(anyString())).thenReturn(1L);

        JobSearchResponse result = jobService.searchJobs("unknown", null);

        assertThat(result.getTotalResults()).isEqualTo(0);
        assertThat(result.getJobs()).isEmpty();
    }

    // ─── getJobDetail ─────────────────────────────────────────────────────────

    @Test
    void getJobDetail_skills_cache_hit_returns_cached_skills() {
        when(genericOps.get("jobs:jobid:id-1")).thenReturn("jobs:search:abc123");
        List<JobResult> jobs = List.of(buildJobResult("id-1", "Java Dev", "ALTEN"));
        when(jobsOps.get("jobs:search:abc123")).thenReturn(jobs);
        when(skillsOps.get("jobs:detail:id-1")).thenReturn(List.of("Java", "Spring Boot", "Kafka"));

        JobDetailResponse detail = jobService.getJobDetail("id-1");

        assertThat(detail.getJobId()).isEqualTo("id-1");
        assertThat(detail.getExtractedSkills()).containsExactly("Java", "Spring Boot", "Kafka");
        assertThat(detail.isFromSkillCache()).isTrue();
        verifyNoInteractions(skillExtractor);
    }

    @Test
    void getJobDetail_skills_cache_miss_calls_ollama_and_caches() {
        when(genericOps.get("jobs:jobid:id-2")).thenReturn("jobs:search:def456");
        JobResult job = buildJobResult("id-2", "Senior Java", "Arrow");
        job.setDescription("Design microservices with Spring Boot, Kafka and PostgreSQL.");
        when(jobsOps.get("jobs:search:def456")).thenReturn(List.of(job));
        when(skillsOps.get("jobs:detail:id-2")).thenReturn(null);
        when(skillExtractor.extract(anyString())).thenReturn(List.of("Kafka", "PostgreSQL", "Spring Boot"));

        JobDetailResponse detail = jobService.getJobDetail("id-2");

        assertThat(detail.getExtractedSkills()).containsExactlyInAnyOrder("Kafka", "PostgreSQL", "Spring Boot");
        assertThat(detail.isFromSkillCache()).isFalse();
        assertThat(detail.getDescription()).isNotBlank();
        verify(skillsOps).set(eq("jobs:detail:id-2"), any(), any());
    }

    @Test
    void getJobDetail_job_not_in_cache_throws_ResourceNotFoundException() {
        when(genericOps.get("jobs:jobid:unknown-id")).thenReturn(null);

        assertThatThrownBy(() -> jobService.getJobDetail("unknown-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("unknown-id");

        verifyNoInteractions(skillExtractor);
    }

    @Test
    void getJobDetail_index_exists_but_job_list_not_found_throws() {
        when(genericOps.get("jobs:jobid:id-3")).thenReturn("jobs:search:expired");
        when(jobsOps.get("jobs:search:expired")).thenReturn(null);

        assertThatThrownBy(() -> jobService.getJobDetail("id-3"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ─── evictCache ───────────────────────────────────────────────────────────

    @Test
    void evictCache_deletes_search_index_and_skill_caches() {
        String searchKey = buildExpectedSearchKey("java", "Morocco");
        List<JobResult> jobs = List.of(
                buildJobResult("id-a", "Job A", "CompA"),
                buildJobResult("id-b", "Job B", "CompB")
        );
        when(jobsOps.get(searchKey)).thenReturn(jobs);

        jobService.evictCache("java", "Morocco");

        verify(genericRedisTemplate).delete("jobs:jobid:id-a");
        verify(genericRedisTemplate).delete("jobs:jobid:id-b");
        verify(skillsRedisTemplate).delete("jobs:detail:id-a");
        verify(skillsRedisTemplate).delete("jobs:detail:id-b");
        verify(jobsRedisTemplate).delete(searchKey);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private JobResult buildJobResult(String id, String title, String company) {
        JobResult job = new JobResult();
        job.setJobId(id);
        job.setTitle(title);
        job.setCompanyName(company);
        job.setLocation("Morocco");
        job.setDescription("Java Spring Boot developer needed.");
        job.setExtensions(List.of("Full-time", "No degree mentioned"));
        return job;
    }

    private String buildExpectedSearchKey(String query, String location) {
        String raw = (query + "|" + (location != null ? location : "")).toLowerCase().trim();
        return "jobs:search:" + org.springframework.util.DigestUtils
                .md5DigestAsHex(raw.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
