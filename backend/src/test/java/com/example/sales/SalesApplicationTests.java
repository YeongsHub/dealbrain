package com.example.sales;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires database and external services - run as integration test only")
class SalesApplicationTests {

	@Test
	void contextLoads() {
	}

}
