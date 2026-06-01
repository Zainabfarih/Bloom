package com.bloom.jobservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"internal.security.gateway-secret=test-gateway-secret"
})
class JobServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}