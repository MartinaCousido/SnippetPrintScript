package com.snippetsearcher.printscript.evaluation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import org.printscript.common.Version;
import org.printscript.runner.ExecutionRunner;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Ejecuta el código con los inputs dados y junta lo que imprime (US-8). Por
// ahora responde al final: el streaming de US-9 y US-10 se suma cuando lo
// explique la cátedra.
@Service
public class EvaluationService {

	private static final Logger log = LoggerFactory.getLogger(EvaluationService.class);

	private final ExecutionRunner runner = new ExecutionRunner();

	public EvaluateResponse evaluate(String code, Version version, List<String> inputs) {
		var outputs = new ArrayList<String>();
		var diagnostics = new ArrayList<Diagnostic>();
		Iterator<String> pending = inputs.iterator();
		var ports = new RuntimePorts(
				new OnlyPrintln(outputs),
				// Cada readInput toma el siguiente input. Si no quedan, la
				// librería lo informa como un error de ejecución.
				() -> pending.hasNext() ? Optional.of(pending.next()) : Optional.empty(),
				// Los snippets no leen variables de entorno.
				name -> Optional.empty());
		// La librería recibe el código línea por línea, sin los saltos de línea.
		long start = System.nanoTime();
		runner.run(code.lines().iterator(), ports, version, failure -> diagnostics.add(Diagnostic.from(failure)));
		log.info("ejecutar {} con {} inputs: {} outputs y {} errores en {} ms", version, inputs.size(), outputs.size(),
				diagnostics.size(), (System.nanoTime() - start) / 1_000_000);
		return new EvaluateResponse(outputs, diagnostics);
	}

	// Solo los println cuentan como outputs. El texto de readInput("Nombre:")
	// sale por print y se descarta (ver "Tests de un snippet" en Decisiones
	// técnicas de SnippetSearcher).
	private record OnlyPrintln(List<String> outputs) implements OutputPrinter {

		@Override
		public void println(String line) {
			outputs.add(line);
		}

		@Override
		public void print(String text) {
		}
	}
}
