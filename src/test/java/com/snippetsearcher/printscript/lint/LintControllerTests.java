package com.snippetsearcher.printscript.lint;

import static org.hamcrest.Matchers.empty;
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

// POST /lint con la librería real.
@WebMvcTest(LintController.class)
@Import({LintService.class, InvalidRulesHandler.class})
class LintControllerTests {

	@Autowired
	private MockMvc mvc;

	private ResultActions lint(String body) throws Exception {
		return mvc.perform(post("/lint").contentType(MediaType.APPLICATION_JSON).content(body));
	}

	@Test
	void reportsEachBrokenRuleWithWhereItIs() throws Exception {
		lint("""
				{"code": "let mi_saludo: string = \\"Hola\\";\\nprintln(mi_saludo);", "version": "1.1",
				 "rules": {"identifier_format": "camel case"}}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnostics[0].rule").value("identifier_format"))
				.andExpect(jsonPath("$.diagnostics[0].message").isString())
				.andExpect(jsonPath("$.diagnostics[0].range.start.line").value(1));
	}

	@Test
	void withoutActiveRulesNothingIsBroken() throws Exception {
		lint("""
				{"code": "let mi_saludo: string = \\"Hola\\";\\nprintln(\\"Hola \\" + mi_saludo);", "version": "1.1"}
				""")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.diagnostics", empty()));
	}

	@Test
	void anExpressionInPrintlnBreaksTheRuleWhenItIsActive() throws Exception {
		lint("""
				{"code": "println(\\"Hola \\" + \\"Ana\\");", "version": "1.1",
				 "rules": {"mandatory-variable-or-literal-in-println": true}}
				""")
				.andExpect(jsonPath("$.diagnostics[0].rule").value("mandatory-variable-or-literal-in-println"));
	}

	@Test
	void aValueThatDoesNotWorkIsABadRequest() throws Exception {
		lint("""
				{"code": "println(1);", "version": "1.1", "rules": {"identifier_format": "kebab case"}}
				""").andExpect(status().isBadRequest());
		lint("""
				{"code": "println(1);", "version": "2.0"}
				""").andExpect(status().isBadRequest());
	}
}
