package com.snippetsearcher.printscript.execution;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Un mensaje del WebSocket /execute, en JSON. type dice cuál es:
//   de Snippet Service: start { code, version, inputs? } e input { value }
//   hacia Snippet Service: output { value }, input-request { prompt },
//   error { diagnostic } y end.
// Ver "Ejecución en vivo" en Decisiones técnicas de SnippetSearcher.
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExecutionMessage(String type, String code, String version, List<String> inputs, String value,
		String prompt, Diagnostic diagnostic) {

	static final String START = "start";
	static final String INPUT = "input";

	static ExecutionMessage output(String line) {
		return new ExecutionMessage("output", null, null, null, line, null, null);
	}

	static ExecutionMessage inputRequest(String prompt) {
		return new ExecutionMessage("input-request", null, null, null, null, prompt, null);
	}

	static ExecutionMessage error(Diagnostic diagnostic) {
		return new ExecutionMessage("error", null, null, null, null, null, diagnostic);
	}

	static ExecutionMessage end() {
		return new ExecutionMessage("end", null, null, null, null, null, null);
	}
}
