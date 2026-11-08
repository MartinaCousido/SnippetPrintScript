package com.snippetsearcher.printscript.format;

import java.util.List;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// content: el código formateado. Si el formateador falló, el código sin
// cambios, y diagnostics dice por qué.
public record FormatResponse(String content, List<Diagnostic> diagnostics) {
}
