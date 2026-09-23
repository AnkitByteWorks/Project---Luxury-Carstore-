package com.Luxurycars.carstore;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")     // ← IMPORTANT: use test profile
class CarstoreApplicationTests {

	@Test
	void contextLoads() {
		// verifies Spring context starts up with test config
	}
}