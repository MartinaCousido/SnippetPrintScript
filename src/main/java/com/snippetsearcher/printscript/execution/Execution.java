package com.snippetsearcher.printscript.execution;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.printscript.common.Version;
import org.printscript.runner.ExecutionRunner;
import org.printscript.runtime.InputProvider;
import org.printscript.runtime.OutputPrinter;
import org.printscript.runtime.RuntimePorts;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

import tools.jackson.databind.json.JsonMapper;

// Una ejecución en vivo (US-9, US-10). Corre en su propio hilo: cada println
// sale apenas ocurre, y cada readInput espera el próximo input.
//   - Con inputs (correr un test, US-9): cada readInput toma el siguiente de la
//     lista y no se le pide nada a nadie. Si se acaban, es un error.
//   - Sin inputs (US-10): cada readInput manda input-request con su texto y
//     espera un input. Si no llega en el tiempo de espera, se corta: se manda
//     el error de la librería (MISSING_INPUT) y después solo el end.
// Solo los println son outputs; el texto de readInput va en input-request.
class Execution implements Runnable {

	private static final Logger log = LoggerFactory.getLogger(Execution.class);

	private final WebSocketSession session;
	private final JsonMapper json;
	private final String code;
	private final Version version;
	private final Iterator<String> givenInputs;
	private final Duration inputTimeout;
	private final String requestId;
	private final BlockingQueue<String> typedInputs = new LinkedBlockingQueue<>();
	private volatile boolean cancelled;
	// Pasó el tiempo de espera sin un input: se manda el error de la librería
	// (MISSING_INPUT, con su línea y columna) y nada más.
	private boolean timedOut;
	private boolean timeoutReported;
	private String prompt = "";
	private int outputs;
	private int errors;

	Execution(WebSocketSession session, JsonMapper json, String code, Version version, List<String> inputs,
			Duration inputTimeout, String requestId) {
		this.session = session;
		this.json = json;
		this.code = code;
		this.version = version;
		this.givenInputs = inputs == null ? null : List.copyOf(inputs).iterator();
		this.inputTimeout = inputTimeout;
		this.requestId = requestId;
	}

	// Un input que escribió el usuario.
	void offer(String input) {
		typedInputs.add(input);
	}

	// Se cerró la conexión: el próximo readInput termina la ejecución.
	void cancel() {
		cancelled = true;
		typedInputs.add("");
	}

	// Cualquier error inesperado cierra la conexión con 1011, para que Snippet
	// Service no quede esperando: por eso se atrapa RuntimeException.
	@Override
	@SuppressWarnings("PMD.AvoidCatchingGenericException")
	public void run() {
		MDC.put("requestId", requestId);
		MDC.put("service", "printscript-service");
		var mode = givenInputs == null ? "interactiva" : "de un test";
		log.info("ejecución en vivo {} con PrintScript {}: empieza", mode, version);
		long start = System.nanoTime();
		try {
			var ports = new RuntimePorts(new Printer(), givenInputs == null ? this::waitForInput : this::nextGivenInput,
					name -> Optional.empty());
			new ExecutionRunner().run(code.lines().iterator(), ports, version, failure -> {
				if (timeoutReported) {
					return;
				}
				timeoutReported = timedOut;
				errors++;
				send(ExecutionMessage.error(Diagnostic.from(failure)));
			});
			send(ExecutionMessage.end());
			log.info("ejecución en vivo {}: termina con {} outputs y {} errores en {} ms", mode, outputs, errors,
					(System.nanoTime() - start) / 1_000_000);
			close(CloseStatus.NORMAL);
		} catch (RuntimeException e) {
			log.error("ejecución en vivo {}: falló", mode, e);
			close(CloseStatus.SERVER_ERROR);
		} finally {
			MDC.remove("requestId");
			MDC.remove("service");
		}
	}

	private Optional<String> nextGivenInput() {
		return givenInputs.hasNext() ? Optional.of(givenInputs.next()) : Optional.empty();
	}

	private Optional<String> waitForInput() {
		// Ya se cortó: un readInput que siga no vuelve a pedir nada.
		if (cancelled || timedOut) {
			return Optional.empty();
		}
		send(ExecutionMessage.inputRequest(prompt));
		try {
			var input = typedInputs.poll(inputTimeout.toMillis(), TimeUnit.MILLISECONDS);
			if (input == null) {
				log.warn("ejecución en vivo: pasaron {} sin un input, se corta", inputTimeout);
				timedOut = true;
				return Optional.empty();
			}
			return cancelled ? Optional.empty() : Optional.of(input);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return Optional.empty();
		}
	}

	private void send(ExecutionMessage message) {
		if (cancelled || !session.isOpen()) {
			return;
		}
		try {
			session.sendMessage(new TextMessage(json.writeValueAsString(message)));
		} catch (IOException e) {
			log.warn("ejecución en vivo: no se pudo mandar un mensaje, se corta", e);
			cancel();
		}
	}

	private void close(CloseStatus status) {
		try {
			if (session.isOpen()) {
				session.close(status);
			}
		} catch (IOException e) {
			log.warn("ejecución en vivo: no se pudo cerrar la conexión", e);
		}
	}

	// println es un output y sale enseguida. print es el texto de readInput:
	// se guarda para mandarlo en el input-request que sigue.
	private final class Printer implements OutputPrinter {

		@Override
		public void println(String line) {
			if (timedOut) {
				return;
			}
			outputs++;
			send(ExecutionMessage.output(line));
		}

		@Override
		public void print(String text) {
			prompt = text;
		}
	}
}
