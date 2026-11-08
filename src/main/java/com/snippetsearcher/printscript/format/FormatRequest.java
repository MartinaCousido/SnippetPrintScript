package com.snippetsearcher.printscript.format;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// rules: solo las reglas activas, con su valor (true en las de sí o no).
public record FormatRequest(@NotNull String code, @NotBlank String version, Map<String, Object> rules) {

	public Map<String, Object> rulesOrEmpty() {
		return rules == null ? Map.of() : rules;
	}
}
