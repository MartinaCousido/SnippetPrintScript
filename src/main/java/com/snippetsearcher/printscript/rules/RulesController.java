package com.snippetsearcher.printscript.rules;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /rules: el contrato está en Decisiones técnicas de SnippetSearcher.
@RestController
public class RulesController {

	private final RuleCatalog catalog;

	public RulesController(RuleCatalog catalog) {
		this.catalog = catalog;
	}

	@GetMapping("/rules")
	public Catalog rules() {
		return new Catalog(catalog.lint(), catalog.format());
	}

	public record Catalog(List<Rule> lint, List<Rule> format) {
	}
}
