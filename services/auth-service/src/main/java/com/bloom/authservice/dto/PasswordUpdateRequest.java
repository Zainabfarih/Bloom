package com.bloom.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordUpdateRequest {
    @NotBlank
    private String token;
    @NotBlank @Size(min = 8)
    private String newPassword;
}