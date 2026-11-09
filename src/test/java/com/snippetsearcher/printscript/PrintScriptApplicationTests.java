package com.snippetsearcher.printscript;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.snippetsearcher.printscript.validation.ValidationService;

@SpringBootTest
class PrintScriptApplicationTests {

	@Autowired
	private ApplicationContext context;

	// El servicio arranca con su configuración y la validación queda lista.
	@Test
	void contextLoads() {
		assertNotNull(context.getBean(ValidationService.class));
	}

}
