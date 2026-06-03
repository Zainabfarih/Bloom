package com.bloom.apigateway;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/fallback")
public class FallbackController {

    @RequestMapping("/job")
    public ResponseEntity<Map<String, String>> jobFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "Job service is temporarily unavailable. Please try again later.",
                        "timestamp", Instant.now().toString()
                ));
    }

    @RequestMapping("/auth")
    public ResponseEntity<Map<String, String>> authFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "Authentication service is temporarily unavailable.",
                        "timestamp", Instant.now().toString()
                ));
    }

    @RequestMapping("/cv")
    public ResponseEntity<Map<String, String>> cvFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "SERVICE_UNAVAILABLE",
                        "message", "CV service is temporarily unavailable.",
                        "timestamp", Instant.now().toString()
                ));
    }
}