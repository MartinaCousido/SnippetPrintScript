package com.snippetsearcher.printscript.validation;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snippetsearcher.printscript.version.PrintScriptVersions;

import jakarta.validation.Valid;

// POST /validate: el contrato está en Decisiones técnicas de SnippetSearcher.
@RestController
public class ValidationController {

	private final ValidationService validation;

	public ValidationController(ValidationService validation) {
		this.validation = validation;
	}

	@PostMapping("/validate")
	public ValidateResponse validate(@Valid @RequestBody ValidateRequest request) {
		return new ValidateResponse(
				validation.validate(request.code(), PrintScriptVersions.parse(request.version())));
	}
}
