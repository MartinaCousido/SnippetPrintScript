package com.snippetsearcher.printscript.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// El código puede estar vacío (un snippet vacío es válido), pero tiene que venir.
public record ValidateRequest(@NotNull String code, @NotBlank String version) {
}
