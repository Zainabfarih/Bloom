package com.bloom.authservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String role;

    // ─── Admin-only enrichment (null for self-profile responses) ───
    private LocalDateTime createdAt;
    private Boolean enabled;
    private Boolean locked;
    private Boolean deleted;
}
