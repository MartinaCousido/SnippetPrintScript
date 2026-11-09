package com.snippetsearcher.printscript.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

// WebSocket /execute (US-9, US-10). El tiempo de espera de un input es corto
// para poder probar el corte.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
		properties = "execution.input-timeout=1s")
class ExecutionTests {

	private static final String GREETING = """
			const nombre: string = readInput("Nombre:");
			println("Hola " + nombre);
			println("Chau");
			""";

	private final JsonMapper json = JsonMapper.builder().build();

	@LocalServerPort
	private int port;

	private Client client;

	@AfterEach
	void close() throws Exception {
		if (client != null && client.session.isOpen()) {
			client.session.close();
		}
	}

	@Test
	void interactiveExecutionAsksForEachInputAndStreamsTheOutputs() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"1.1\", \"code\": " + json.writeValueAsString(GREETING) + "}");

		var request = client.next();
		assertThat(request.get("type").asString()).isEqualTo("input-request");
		assertThat(request.get("prompt").asString()).isEqualTo("Nombre:");

		client.send("{\"type\": \"input\", \"value\": \"Ana\"}");
		assertThat(client.next().get("value").asString()).isEqualTo("Hola Ana");
		assertThat(client.next().get("value").asString()).isEqualTo("Chau");
		assertThat(client.next().get("type").asString()).isEqualTo("end");
		assertThat(client.closed.get(5, TimeUnit.SECONDS)).isEqualTo(CloseStatus.NORMAL);
	}

	@Test
	void anOutputArrivesBeforeTheProgramAsksForTheNextInput() throws Exception {
		client = connect();
		var code = """
				println("primero");
				const x: string = readInput("x:");
				println(x);
				""";
		client.send("{\"type\": \"start\", \"version\": \"1.1\", \"code\": " + json.writeValueAsString(code) + "}");

		var first = client.next();
		assertThat(first.get("type").asString()).isEqualTo("output");
		assertThat(first.get("value").asString()).isEqualTo("primero");
		assertThat(client.next().get("type").asString()).isEqualTo("input-request");
	}

	@Test
	void aTestUsesItsInputsWithoutAskingForThem() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"1.1\", \"inputs\": [\"Leo\"], \"code\": "
				+ json.writeValueAsString(GREETING) + "}");

		var first = client.next();
		assertThat(first.get("type").asString()).isEqualTo("output");
		assertThat(first.get("value").asString()).isEqualTo("Hola Leo");
		assertThat(client.next().get("value").asString()).isEqualTo("Chau");
		assertThat(client.next().get("type").asString()).isEqualTo("end");
	}

	@Test
	void runningOutOfInputsIsAnError() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"1.1\", \"inputs\": [], \"code\": "
				+ json.writeValueAsString(GREETING) + "}");

		var error = client.next();
		assertThat(error.get("type").asString()).isEqualTo("error");
		assertThat(error.get("diagnostic").get("rule").asString()).isEqualTo("MISSING_INPUT");
		assertThat(error.get("diagnostic").get("range").get("start").get("line").asInt()).isEqualTo(1);
		client.skipUntilEnd();
	}

	@Test
	void withoutAnInputInTimeTheExecutionIsCut() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"1.1\", \"code\": " + json.writeValueAsString(GREETING) + "}");

		assertThat(client.next().get("type").asString()).isEqualTo("input-request");
		var error = client.next();
		assertThat(error.get("type").asString()).isEqualTo("error");
		assertThat(error.get("diagnostic").get("rule").asString()).isEqualTo("MISSING_INPUT");
		assertThat(client.next().get("type").asString()).isEqualTo("end");
	}

	@Test
	void invalidCodeSendsItsErrorsAndEnds() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"1.1\", \"code\": \"println(1)\"}");

		var error = client.next();
		assertThat(error.get("type").asString()).isEqualTo("error");
		assertThat(error.get("diagnostic").get("rule").asString()).isNotBlank();
		client.skipUntilEnd();
	}

	@Test
	void anUnknownVersionClosesTheConnection() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"9.9\", \"code\": \"println(1);\"}");

		var status = client.closed.get(5, TimeUnit.SECONDS);
		assertThat(status.getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
		assertThat(status.getReason()).contains("9.9");
	}

	@Test
	void anInputBeforeStartClosesTheConnection() throws Exception {
		client = connect();
		client.send("{\"type\": \"input\", \"value\": \"Ana\"}");

		assertThat(client.closed.get(5, TimeUnit.SECONDS).getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
	}

	@Test
	void aMessageThatIsNotJsonClosesTheConnection() throws Exception {
		client = connect();
		client.send("hola");

		assertThat(client.closed.get(5, TimeUnit.SECONDS).getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
	}

	@Test
	void aSecondStartClosesTheConnection() throws Exception {
		client = connect();
		var start = "{\"type\": \"start\", \"version\": \"1.1\", \"code\": " + json.writeValueAsString(GREETING) + "}";
		client.send(start);
		assertThat(client.next().get("type").asString()).isEqualTo("input-request");

		client.send(start);

		assertThat(client.closed.get(5, TimeUnit.SECONDS).getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
	}

	@Test
	void aStartWithoutCodeClosesTheConnection() throws Exception {
		client = connect();
		client.send("{\"type\": \"start\", \"version\": \"1.1\"}");

		assertThat(client.closed.get(5, TimeUnit.SECONDS).getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
	}

	@Test
	void anUnknownMessageTypeClosesTheConnection() throws Exception {
		client = connect();
		client.send("{\"type\": \"pausa\"}");

		assertThat(client.closed.get(5, TimeUnit.SECONDS).getCode()).isEqualTo(CloseStatus.BAD_DATA.getCode());
	}

	private Client connect() throws Exception {
		var client = new Client();
		var headers = new WebSocketHttpHeaders();
		headers.add("X-Request-Id", "test-ejecucion");
		client.session = new StandardWebSocketClient()
				.execute(client, headers, URI.create("ws://localhost:" + port + "/execute"))
				.get(5, TimeUnit.SECONDS);
		return client;
	}

	private final class Client extends TextWebSocketHandler {

		private final BlockingQueue<String> messages = new LinkedBlockingQueue<>();
		private final CompletableFuture<CloseStatus> closed = new CompletableFuture<>();
		private WebSocketSession session;

		@Override
		protected void handleTextMessage(WebSocketSession session, TextMessage message) {
			messages.add(message.getPayload());
		}

		@Override
		public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
			closed.complete(status);
		}

		void send(String message) throws Exception {
			session.sendMessage(new TextMessage(message));
		}

		// Después de un error la librería puede seguir (más errores, o los println
		// que quedan, como en /evaluate): se pasan hasta el end, que tiene que llegar.
		void skipUntilEnd() throws Exception {
			var message = next();
			while (!"end".equals(message.get("type").asString())) {
				message = next();
			}
			assertThat(message.get("type").asString()).isEqualTo("end");
		}

		JsonNode next() throws Exception {
			var message = messages.poll(5, TimeUnit.SECONDS);
			assertThat(message).as("llegó un mensaje").isNotNull();
			return json.readTree(message);
		}
	}
}
