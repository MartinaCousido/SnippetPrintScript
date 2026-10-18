package com.snippetsearcher.printscript.version;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// Una versión desconocida es un error de quien llama, no del servicio: responde 400.
@RestControllerAdvice
public class UnsupportedVersionHandler {

	@ExceptionHandler(UnsupportedVersionException.class)
	ProblemDetail handle(UnsupportedVersionException exception) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
	}
}
