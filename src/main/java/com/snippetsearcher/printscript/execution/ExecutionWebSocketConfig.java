package com.snippetsearcher.printscript.execution;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

// WebSocket /execute: ejecutar un snippet en vivo (US-9, US-10). Lo usa solo
// Snippet Service, por la red interna. Ver "Ejecución en vivo" en Decisiones
// técnicas de SnippetSearcher.
@Configuration
@EnableWebSocket
public class ExecutionWebSocketConfig implements WebSocketConfigurer {

	private final ExecutionHandler handler;

	public ExecutionWebSocketConfig(ExecutionHandler handler) {
		this.handler = handler;
	}

	@Override
	public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		registry.addHandler(handler, "/execute").addInterceptors(new RequestIdHandshakeInterceptor());
	}
}
