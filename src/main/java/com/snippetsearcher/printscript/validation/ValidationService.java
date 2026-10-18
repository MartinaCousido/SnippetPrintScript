package com.snippetsearcher.printscript.validation;

import java.util.List;

import org.printscript.common.Version;
import org.printscript.runner.ValidationRunner;
import org.springframework.stereotype.Service;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Revisa sintaxis y semántica sin ejecutar el código.
@Service
public class ValidationService {

	private final ValidationRunner runner = new ValidationRunner();

	public List<Diagnostic> validate(String code, Version version) {
		// La librería recibe el código línea por línea, sin los saltos de línea.
		return runner.validate(code.lines().iterator(), version).stream()
				.map(Diagnostic::from)
				.toList();
	}
}
