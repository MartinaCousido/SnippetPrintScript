package com.snippetsearcher.printscript.evaluation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snippetsearcher.printscript.version.PrintScriptVersions;

import jakarta.validation.Valid;

// POST /evaluate: el contrato está en Decisiones técnicas de SnippetSearcher.
@RestController
public class EvaluationController {

	private final EvaluationService evaluation;

	public EvaluationController(EvaluationService evaluation) {
		this.evaluation = evaluation;
	}

	@PostMapping("/evaluate")
	public EvaluateResponse evaluate(@Valid @RequestBody EvaluateRequest request) {
		return evaluation.evaluate(request.code(), PrintScriptVersions.parse(request.version()),
				request.inputsOrEmpty());
	}
}
