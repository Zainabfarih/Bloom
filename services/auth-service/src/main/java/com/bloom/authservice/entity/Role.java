package com.bloom.authservice.entity;

public enum Role {
    STUDENT,
    ADMIN;

    public java.util.Set<org.springframework.security.core.authority.SimpleGrantedAuthority> getAuthorities() {
        return java.util.Set.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_" + this.name()));
    }
}