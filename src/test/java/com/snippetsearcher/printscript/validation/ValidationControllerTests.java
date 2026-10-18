package com.snippetsearcher.printscript.validation;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

// Prueba POST /validate con la librería real: lo que importa es que los
// errores de PrintScript lleguen con el formato de Diagnostic.
@WebMvcTest(ValidationController.class)
@Import(ValidationService.class)
class ValidationControllerTests {

	@Autowired
	private MockMvc mvc;

	private ResultActions validate(String body) throws Exception {
		return mvc.perform(post("/validate").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void validCodeHasNoDiagnostics() throws Exception {
		validate("""
				{"code": "let saludo: string = \\"Hola\\";\\nprintln(saludo);", "version": "1.0"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnostics", empty()));
	}

	@Test
	void aMissingSemicolonReportsTheRuleAndWhereItIs() throws Exception {
		// Sin ';' la declaración sigue en la línea 2: la librería marca el token
		// donde esperaba el ';', que es el println (línea 2, columnas 1 a 7).
		validate("""
				{"code": "let saludo: string = \\"Hola\\"\\nprintln(saludo);", "version": "1.0"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnostics.length()").value(1))
				.andExpect(jsonPath("$.diagnostics[0].rule").value("MISSING_SEMICOLON"))
				.andExpect(jsonPath("$.diagnostics[0].message").isString())
				.andExpect(jsonPath("$.diagnostics[0].range.start.line").value(2))
				.andExpect(jsonPath("$.diagnostics[0].range.start.column").value(1))
				.andExpect(jsonPath("$.diagnostics[0].range.end.line").value(2))
				.andExpect(jsonPath("$.diagnostics[0].range.end.column").value(7));
	}

	@Test
	void theVersionDecidesWhatIsValid() throws Exception {
		// const existe recién en PrintScript 1.1.
		String constCode = """
				{"code": "const edad: number = 30;", "version": "%s"}
				""";
		validate(constCode.formatted("1.1")).andExpect(jsonPath("$.diagnostics", empty()));
		validate(constCode.formatted("1.0")).andExpect(jsonPath("$.diagnostics", not(empty())));
	}

	@Test
	void emptyCodeIsValid() throws Exception {
		validate("""
				{"code": "", "version": "1.1"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnostics", empty()));
	}

	@Test
	void anUnknownVersionIsABadRequest() throws Exception {
		validate("""
				{"code": "println(1);", "version": "2.0"}
				""")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.detail").value("Versión de PrintScript no soportada: 2.0"));
	}

	@Test
	void missingFieldsAreABadRequest() throws Exception {
		validate("""
				{"version": "1.1"}
				""").andExpect(status().isBadRequest());
		validate("""
				{"code": "println(1);"}
				""").andExpect(status().isBadRequest());
	}
}
