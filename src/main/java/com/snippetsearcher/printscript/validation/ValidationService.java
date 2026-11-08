package com.snippetsearcher.printscript.validation;

import java.util.List;

import org.printscript.common.Version;
import org.printscript.runner.ValidationRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Revisa sintaxis y semántica sin ejecutar el código.
@Service
public class ValidationService {

	private static final Logger log = LoggerFactory.getLogger(ValidationService.class);

	private final ValidationRunner runner = new ValidationRunner();

	public List<Diagnostic> validate(String code, Version version) {
		long start = System.nanoTime();
		// La librería recibe el código línea por línea, sin los saltos de línea.
		var diagnostics = runner.validate(code.lines().iterator(), version).stream()
				.map(Diagnostic::from)
				.toList();
		log.info("validar {}: {} errores en {} ms", version, diagnostics.size(), (System.nanoTime() - start) / 1_000_000);
		return diagnostics;
	}
}
