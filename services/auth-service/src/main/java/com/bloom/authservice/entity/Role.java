package com.bloom.authservice.entity;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Set;

public enum Role {
    STUDENT, ADMIN;

    public Set<SimpleGrantedAuthority> getAuthorities() {
        return Set.of(new SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}