package com.bloom.roadmapservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class RoadmapServiceApplicationTests {

    /**
     * Smoke test : vérifie que le contexte Spring se charge complètement
     * (config, beans, JPA, sécurité) avec la configuration de test.
     */
    @Test
    void contextLoads() {
    }
}
