package com.bloom.roadmapservice.external;

import com.bloom.roadmapservice.dto.SkillGapResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class JobServiceClientFallback implements JobServiceClient {

    @Override
    public SkillGapResponse getJobSkillGap(Long userId, Long targetJobId,
                                           String xUserId, String gatewaySecret) {
        log.warn("job-service fallback — userId={}, jobId={}", userId, targetJobId);
        return new SkillGapResponse(userId, targetJobId, null, List.of());
    }
}