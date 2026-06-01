package com.bloom.apigateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"jwt.secret=5468697349734153656372657454686973497341536563726574546869734153",
		"internal.security.gateway-secret=test-gateway-secret",
		"eureka.client.enabled=false",
		"eureka.client.register-with-eureka=false",
		"eureka.client.fetch-registry=false",
		"spring.cloud.config.enabled=false",
		"spring.config.import="
})
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}