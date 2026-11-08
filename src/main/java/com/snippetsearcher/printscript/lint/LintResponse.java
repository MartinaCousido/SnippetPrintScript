package com.snippetsearcher.printscript.lint;

import java.util.List;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Cada regla incumplida. Lista vacía = el código cumple todas las reglas activas.
public record LintResponse(List<Diagnostic> diagnostics) {
}
