package com.bloom.authservice.mapper;

import com.bloom.authservice.dto.UserDTO;
import com.bloom.authservice.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserDTO toDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }

    /**
     * Admin-enriched DTO — includes account status + audit fields.
     * Used only for ADMIN-scoped endpoints (user management table & analytics).
     */
    public UserDTO toAdminDTO(User user) {
        if (user == null) return null;

        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .createdAt(user.getCreatedAt())
                .enabled(user.isEnabled())
                .locked(user.isLocked())
                .deleted(user.isDeleted())
                .build();
    }

}