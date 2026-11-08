package com.snippetsearcher.printscript.rules;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Una regla con un valor que la librería no acepta (por ejemplo, un texto
// donde va un número, o un formato de identificador que no existe) es un
// error de quien llama: 400 con el mensaje de la librería.
@RestControllerAdvice
public class InvalidRulesHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	ProblemDetail handle(IllegalArgumentException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
