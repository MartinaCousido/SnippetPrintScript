package com.snippetsearcher.printscript.version;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

// GET /version responde la versión de la librería que está en el classpath.
@SpringBootTest
@AutoConfigureMockMvc
class VersionControllerTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void respondsTheLibraryVersion() throws Exception {
		mvc.perform(get("/version"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.library").value("0.3.2"));
	}
}
