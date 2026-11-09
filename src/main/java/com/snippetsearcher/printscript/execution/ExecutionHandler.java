package com.snippetsearcher.printscript.execution;

import java.io.IOException;
import java.time.Duration;

import org.printscript.common.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.snippetsearcher.printscript.version.PrintScriptVersions;
import com.snippetsearcher.printscript.version.UnsupportedVersionException;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

// Atiende el WebSocket /execute. Cada conexión hace una sola ejecución: el
// primer mensaje (start) trae el código, y después pueden llegar inputs. Un
// mensaje que no corresponde cierra la conexión con 1007 (BAD_DATA) y el motivo.
@Component
public class ExecutionHandler extends TextWebSocketHandler {

	private static final Logger log = LoggerFactory.getLogger(ExecutionHandler.class);
	private static final String EXECUTION = "execution";
	private static final String SENDER = "sender";
	// Cuánto puede tardar en salir un mensaje y cuánto se acumula si la otra
	// punta lee lento, antes de cortar la conexión.
	private static final int SEND_TIME_LIMIT_MS = 10_000;
	private static final int BUFFER_SIZE_LIMIT = 1024 * 1024;
	// El código de un snippet viaja en el primer mensaje. Se acepta hasta 5 MB,
	// lo mismo que acepta el proxy al crear un snippet.
	static final int MAX_MESSAGE_BYTES = 5 * 1024 * 1024;

	private final JsonMapper json;
	private final Duration inputTimeout;

	public ExecutionHandler(JsonMapper json, @Value("${execution.input-timeout}") Duration inputTimeout) {
		this.json = json;
		this.inputTimeout = inputTimeout;
	}

	@Override
	public void afterConnectionEstablished(WebSocketSession session) {
		session.setTextMessageSizeLimit(MAX_MESSAGE_BYTES);
		// Los mensajes salen desde el hilo de la ejecución: el decorador evita que
		// dos envíos se pisen.
		session.getAttributes().put(SENDER,
				new ConcurrentWebSocketSessionDecorator(session, SEND_TIME_LIMIT_MS, BUFFER_SIZE_LIMIT));
	}

	@Override
	protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
		ExecutionMessage received;
		try {
			received = json.readValue(message.getPayload(), ExecutionMessage.class);
		} catch (JacksonException e) {
			reject(session, "El mensaje no es JSON válido");
			return;
		}
		var execution = (Execution) session.getAttributes().get(EXECUTION);
		if (ExecutionMessage.START.equals(received.type())) {
			start(session, execution, received);
		} else if (ExecutionMessage.INPUT.equals(received.type())) {
			if (execution == null) {
				reject(session, "Primero hay que mandar start");
				return;
			}
			execution.offer(received.value() == null ? "" : received.value());
		} else {
			reject(session, "Tipo de mensaje desconocido: " + received.type());
		}
	}

	private void start(WebSocketSession session, Execution current, ExecutionMessage start) throws IOException {
		if (current != null) {
			reject(session, "Ya hay una ejecución en esta conexión");
			return;
		}
		if (start.code() == null) {
			reject(session, "Falta el código");
			return;
		}
		Version version;
		try {
			version = PrintScriptVersions.parse(start.version());
		} catch (UnsupportedVersionException e) {
			reject(session, e.getMessage());
			return;
		}
		var requestId = (String) session.getAttributes().get(RequestIdHandshakeInterceptor.REQUEST_ID);
		var execution = new Execution((WebSocketSession) session.getAttributes().get(SENDER), json, start.code(),
				version, start.inputs(), inputTimeout, requestId);
		session.getAttributes().put(EXECUTION, execution);
		Thread.ofVirtual().name("execution-" + requestId).start(execution);
	}

	@Override
	public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
		var execution = (Execution) session.getAttributes().get(EXECUTION);
		if (execution != null) {
			execution.cancel();
		}
	}

	private static void reject(WebSocketSession session, String reason) throws IOException {
		log.warn("ejecución en vivo rechazada: {}", reason);
		session.close(CloseStatus.BAD_DATA.withReason(reason));
	}
}
