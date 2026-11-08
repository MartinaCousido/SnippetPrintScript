package com.snippetsearcher.printscript.evaluation;

import static org.hamcrest.Matchers.contains;
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

// Prueba POST /evaluate con la librería real.
@WebMvcTest(EvaluationController.class)
@Import(EvaluationService.class)
class EvaluationControllerTests {

	// Pide dos inputs e imprime según el segundo. PrintScript no tiene
	// comparaciones: el if recibe un booleano.
	private static final String SALUDO = """
			const nombre: string = readInput("Nombre:");
			const mayor: boolean = readInput("¿Mayor de edad?");
			println("Hola " + nombre);
			if (mayor) {
			  println("Sos mayor de edad");
			} else {
			  println("Sos menor de edad");
			}
			""";

	@Autowired
	private MockMvc mvc;

	private ResultActions evaluate(String body) throws Exception {
		return mvc.perform(post("/evaluate").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	// El código como texto de JSON: comillas y saltos de línea escapados.
	private static String json(String code) {
		return code.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
	}

	@Test
	void returnsWhatEachPrintlnPrintedInOrder() throws Exception {
		evaluate("""
				{"code": "let a: number = 1;\\nprintln(a);\\nprintln(\\"chau\\");", "version": "1.0"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outputs", contains("1", "chau")))
				.andExpect(jsonPath("$.diagnostics", empty()));
	}

	@Test
	void eachReadInputTakesTheNextInputAndItsTextIsNotAnOutput() throws Exception {
		evaluate("""
				{"code": "%s", "version": "1.1", "inputs": ["Ana", "true"]}
				""".formatted(json(SALUDO)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outputs", contains("Hola Ana", "Sos mayor de edad")))
				.andExpect(jsonPath("$.diagnostics", empty()));
		evaluate("""
				{"code": "%s", "version": "1.1", "inputs": ["Leo", "false"]}
				""".formatted(json(SALUDO)))
				.andExpect(jsonPath("$.outputs", contains("Hola Leo", "Sos menor de edad")));
	}

	@Test
	void runningOutOfInputsIsAnErrorWithWhatWasPrintedSoFar() throws Exception {
		evaluate("""
				{"code": "println(\\"antes\\");\\nconst n: string = readInput(\\"Nombre:\\");\\nprintln(n);", "version": "1.1"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outputs", contains("antes")))
				.andExpect(jsonPath("$.diagnostics", not(empty())))
				.andExpect(jsonPath("$.diagnostics[0].rule").isString())
				.andExpect(jsonPath("$.diagnostics[0].range.start.line").isNumber());
	}

	@Test
	void invalidCodeIsNotExecuted() throws Exception {
		evaluate("""
				{"code": "println(\\"hola\\")\\nprintln(\\"chau\\");", "version": "1.0"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outputs", empty()))
				.andExpect(jsonPath("$.diagnostics[0].rule").value("MISSING_SEMICOLON"));
	}

	@Test
	void anUnknownVersionOrAMissingFieldIsABadRequest() throws Exception {
		evaluate("""
				{"code": "println(1);", "version": "2.0"}
				""").andExpect(status().isBadRequest());
		evaluate("""
				{"version": "1.1"}
				""").andExpect(status().isBadRequest());
	}
}
