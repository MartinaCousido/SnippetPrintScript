package com.snippetsearcher.printscript.observability;

import java.io.IOException;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

// Carga el contexto de cada pedido en los logs (MDC): el requestId que manda
// Snippet Service (X-Request-Id), para ver juntos los logs de un mismo pedido,
// y el servicio. El agente de New Relic los manda como atributos.
@Component("logContextFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestContextFilter extends OncePerRequestFilter {

	static final String REQUEST_ID_HEADER = "X-Request-Id";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		var requestId = request.getHeader(REQUEST_ID_HEADER);
		if (requestId == null || requestId.isBlank() || requestId.length() > 100) {
			requestId = UUID.randomUUID().toString();
		}
		MDC.put("requestId", requestId);
		MDC.put("service", "printscript-service");
		response.setHeader(REQUEST_ID_HEADER, requestId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove("requestId");
			MDC.remove("service");
		}
	}
}
