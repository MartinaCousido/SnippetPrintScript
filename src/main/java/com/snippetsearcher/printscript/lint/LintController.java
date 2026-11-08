package com.snippetsearcher.printscript.lint;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snippetsearcher.printscript.version.PrintScriptVersions;

import jakarta.validation.Valid;

// POST /lint: el contrato está en Decisiones técnicas de SnippetSearcher.
@RestController
public class LintController {

	private final LintService lint;

	public LintController(LintService lint) {
		this.lint = lint;
	}

	@PostMapping("/lint")
	public LintResponse lint(@Valid @RequestBody LintRequest request) {
		return new LintResponse(
				lint.lint(request.code(), PrintScriptVersions.parse(request.version()), request.rulesOrEmpty()));
	}
}
