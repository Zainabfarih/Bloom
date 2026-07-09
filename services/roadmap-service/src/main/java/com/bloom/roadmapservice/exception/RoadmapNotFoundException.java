package com.bloom.roadmapservice.exception;

public class RoadmapNotFoundException extends RuntimeException {
    public RoadmapNotFoundException(Long userId, Long jobId) {
        super("Roadmap not found for userId=" + userId + ", jobId=" + jobId);
    }
}