package com.snippetsearcher.printscript.format;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.snippetsearcher.printscript.version.PrintScriptVersions;

import jakarta.validation.Valid;

// POST /format: el contrato está en Decisiones técnicas de SnippetSearcher.
@RestController
public class FormatController {

	private final FormatService format;

	public FormatController(FormatService format) {
		this.format = format;
	}

	@PostMapping("/format")
	public FormatResponse format(@Valid @RequestBody FormatRequest request) {
		return format.format(request.code(), PrintScriptVersions.parse(request.version()), request.rulesOrEmpty());
	}
}
