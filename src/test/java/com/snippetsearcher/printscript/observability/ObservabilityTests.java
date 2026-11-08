package com.snippetsearcher.printscript.observability;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

// El requestId que manda Snippet Service y el log de cada operación, sin el
// código del snippet.
@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(OutputCaptureExtension.class)
class ObservabilityTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void keepsTheRequestIdAndLogsTheOperationWithoutTheCode(CapturedOutput output) throws Exception {
		mvc.perform(post("/validate").header("X-Request-Id", "del-snippet-service")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{\"code\": \"println(\\\"secreto-del-usuario\\\");\", \"version\": \"1.1\"}"))
				.andExpect(header().string("X-Request-Id", "del-snippet-service"));

		assertThat(output).contains("validar V11: 0 errores en").doesNotContain("secreto-del-usuario");
	}
}
