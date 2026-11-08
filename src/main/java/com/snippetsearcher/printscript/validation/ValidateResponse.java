package com.snippetsearcher.printscript.validation;

import java.util.List;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Lista vacía = el código es válido.
public record ValidateResponse(List<Diagnostic> diagnostics) {
}
