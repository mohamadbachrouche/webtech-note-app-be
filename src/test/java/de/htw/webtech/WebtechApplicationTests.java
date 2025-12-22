package de.htw.webtech;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// We manually overwrite the database settings just for this test
@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:testdb",
		"spring.datasource.driverClassName=org.h2.Driver",
		"spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
class WebtechApplicationTests {

	@Test
	void contextLoads() {
	}

}