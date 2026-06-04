package com.bloom.roadmapservice.exception;

public class StepNotFoundException extends RuntimeException {
    public StepNotFoundException(Long stepId) {
        super("Step not found: id=" + stepId);
    }
}