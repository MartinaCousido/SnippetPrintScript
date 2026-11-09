package com.snippetsearcher.printscript.execution;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

// Guarda en la sesión el requestId que manda Snippet Service al abrir el
// WebSocket (X-Request-Id). Los mensajes llegan en otros hilos, sin el MDC del
// pedido HTTP, así que la ejecución lo vuelve a cargar desde acá.
class RequestIdHandshakeInterceptor implements HandshakeInterceptor {

	static final String REQUEST_ID = "requestId";

	@Override
	public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
			Map<String, Object> attributes) {
		var requestId = request.getHeaders().getFirst("X-Request-Id");
		if (requestId == null || requestId.isBlank() || requestId.length() > 100) {
			requestId = UUID.randomUUID().toString();
		}
		attributes.put(REQUEST_ID, requestId);
		return true;
	}

	@Override
	public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
			Exception exception) {
		// Nada que hacer después del handshake.
	}
}
