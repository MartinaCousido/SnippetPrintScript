package com.snippetsearcher.printscript.rules;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RulesController.class)
@Import(RuleCatalog.class)
class RulesControllerTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void listsTheRulesOfTheLibraryWithTheirTypeAndDefault() throws Exception {
		mvc.perform(get("/rules"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lint", hasSize(3)))
				.andExpect(jsonPath("$.format", hasSize(11)))
				.andExpect(jsonPath("$.lint[0].name").value("identifier_format"))
				.andExpect(jsonPath("$.lint[0].type").value("text"))
				.andExpect(jsonPath("$.lint[0].options", contains("camel case", "snake case")))
				.andExpect(jsonPath("$.lint[0].default.isActive").value(true))
				.andExpect(jsonPath("$.lint[0].default.value").value("camel case"))
				.andExpect(jsonPath("$.lint[1].type").value("boolean"))
				.andExpect(jsonPath("$.lint[1].default.value").doesNotExist())
				.andExpect(jsonPath("$.format[?(@.name == 'indent-inside-if')].type").value("number"))
				.andExpect(jsonPath("$.format[?(@.name == 'indent-inside-if')].default.value").value(4))
				.andExpect(jsonPath("$.format[?(@.name == 'if-brace-below-line')].default.isActive").value(false))
				.andExpect(jsonPath("$.format[?(@.name == 'if-brace-below-line')].opposite").value("if-brace-same-line"))
				.andExpect(jsonPath("$.lint[0].opposite").doesNotExist());
	}
}
