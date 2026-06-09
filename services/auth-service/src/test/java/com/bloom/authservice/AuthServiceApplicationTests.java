package com.bloom.authservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class AuthServiceApplicationTests {

    /**
     * Smoke test : vérifie que le contexte Spring se charge complètement
     * (config, beans, JPA, Spring Security) avec la configuration de test.
     */
    @Test
    void contextLoads() {
    }
}
