package com.snippetsearcher.printscript.evaluation;

import java.util.List;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// outputs: lo que imprimió cada println, en orden. Si la ejecución falló,
// diagnostics dice por qué, y outputs tiene lo que alcanzó a imprimir.
public record EvaluateResponse(List<String> outputs, List<Diagnostic> diagnostics) {
}
