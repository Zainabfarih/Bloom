package com.bloom.jobservice.external;

import com.bloom.jobservice.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "auth-service")
public interface AuthServiceClient {

    @GetMapping("/api/auth/users/{id}")
    UserDTO getUserProfile(
            @PathVariable("id") Long id,
            @RequestHeader("Authorization") String bearerToken
    );
}