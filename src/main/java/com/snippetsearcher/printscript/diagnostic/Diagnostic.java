package com.snippetsearcher.printscript.diagnostic;

import org.printscript.common.Failure;
import org.printscript.linter.Violation;

// Un problema en el código, con el formato que comparten todos los lenguajes
// (ver "Diagnostic" en Decisiones técnicas de SnippetSearcher): la regla
// incumplida, un mensaje para la persona y dónde está.
public record Diagnostic(String rule, String message, Range range) {

	public record Range(Position start, Position end) {
	}

	// Línea y columna empiezan en 1, igual que en la librería.
	public record Position(int line, int column) {
	}

	// Una regla de lint incumplida. rule es el nombre con el que se configura.
	public static Diagnostic from(Violation violation) {
		return new Diagnostic(violation.rule(), violation.message(), rangeOf(violation.range()));
	}

	private static Range rangeOf(org.printscript.common.Range range) {
		return new Range(
				new Position(range.start().line(), range.start().column()),
				new Position(range.end().line(), range.end().column()));
	}

	// La regla sale del código de la librería, no del texto del mensaje:
	// el mensaje puede cambiar, el código no.
	public static Diagnostic from(Failure<?> failure) {
		org.printscript.common.Range range = failure.range();
		return new Diagnostic(
				failure.code().name(),
				failure.message(),
				new Range(
						new Position(range.start().line(), range.start().column()),
						new Position(range.end().line(), range.end().column())));
	}
}
