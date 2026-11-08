package com.snippetsearcher.printscript.evaluation;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// inputs es opcional: un snippet sin readInput no los necesita.
public record EvaluateRequest(@NotNull String code, @NotBlank String version, List<String> inputs) {

	public List<String> inputsOrEmpty() {
		return inputs == null ? List.of() : inputs;
	}
}
