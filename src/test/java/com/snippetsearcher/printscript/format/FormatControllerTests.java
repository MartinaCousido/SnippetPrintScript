package com.snippetsearcher.printscript.format;

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

import com.snippetsearcher.printscript.rules.InvalidRulesHandler;

// POST /format con la librería real.
@WebMvcTest(FormatController.class)
@Import({FormatService.class, InvalidRulesHandler.class})
class FormatControllerTests {

	// Los valores por defecto del catálogo.
	private static final String DEFAULTS = """
			{"enforce-spacing-after-colon-in-declaration": true, "enforce-spacing-around-equals": true,
			 "mandatory-space-surrounding-operations": true,
			 "mandatory-line-break-after-statement": true, "if-brace-same-line": true,
			 "line-breaks-after-println": 0, "indent-inside-if": 4}""";

	@Autowired
	private MockMvc mvc;

	private ResultActions format(String body) throws Exception {
		return mvc.perform(post("/format").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	// Con los valores por defecto, el estilo del ejemplo del README de SnippetSearcher.
	@Test
	void withTheDefaultRulesItFormatsTheReadmeExample() throws Exception {
		format("""
				{"code": "let edad:number=readInput(\\"Edad:\\");println(edad);", "version": "1.1", "rules": %s}
				""".formatted(DEFAULTS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("let edad: number = readInput(\"Edad:\");\nprintln(edad);"))
				.andExpect(jsonPath("$.diagnostics", empty()));
	}

	// Desde la librería 0.3.1: el cuerpo de un if escrito en una línea baja de
	// línea y se indenta, y el else queda al lado del '}'.
	@Test
	void withTheDefaultRulesItFormatsAnIfWrittenInOneLine() throws Exception {
		format("""
				{"code": "const m:boolean=true;if(m){println(\\"si\\");}else{println(\\"no\\");}", "version": "1.1", "rules": %s}
				""".formatted(DEFAULTS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value(
						"const m: boolean = true;\nif(m) {\n    println(\"si\");\n} else {\n    println(\"no\");\n}"))
				.andExpect(jsonPath("$.diagnostics", empty()));
	}

	@Test
	void withoutRulesTheCodeIsTheSame() throws Exception {
		format("""
				{"code": "let a:number=1;", "version": "1.1"}
				""")
				.andExpect(jsonPath("$.content").value("let a:number=1;"));
	}

	@Test
	void ifTheFormatterFailsTheCodeIsUnchanged() throws Exception {
		format("""
				{"code": "let a: number = 1 # 2;", "version": "1.1", "rules": %s}
				""".formatted(DEFAULTS))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("let a: number = 1 # 2;"))
				.andExpect(jsonPath("$.diagnostics", not(empty())));
	}

	@Test
	void aValueThatDoesNotWorkIsABadRequest() throws Exception {
		format("""
				{"code": "println(1);", "version": "1.1", "rules": {"indent-inside-if": -1}}
				""").andExpect(status().isBadRequest());
		format("""
				{"code": "println(1);", "version": "1.1", "rules": {"indent-inside-if": "cuatro"}}
				""").andExpect(status().isBadRequest());
	}
}
