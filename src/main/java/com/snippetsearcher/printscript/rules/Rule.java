package com.snippetsearcher.printscript.rules;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;

// Una regla del catálogo (GET /rules). type: boolean (sin valor), text (con
// options) o number. opposite: la regla contraria, que no puede estar activa a
// la vez. defaultValue es lo que usa un usuario que no la cambió
// (ver "Reglas: catálogo, valores por defecto y validación" en Decisiones
// técnicas de SnippetSearcher).
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Rule(String name, Type type, List<String> options, String opposite,
		@JsonProperty("default") Default defaultValue) {

	public enum Type {
		BOOLEAN("boolean"),
		TEXT("text"),
		NUMBER("number");

		private final String value;

		Type(String value) {
			this.value = value;
		}

		@JsonValue
		public String value() {
			return value;
		}
	}

	@JsonInclude(JsonInclude.Include.ALWAYS)
	public record Default(boolean isActive, Object value) {
	}

	static Rule bool(String name, boolean active) {
		return new Rule(name, Type.BOOLEAN, null, null, new Default(active, null));
	}

	static Rule opposite(String name, String opposite, boolean active) {
		return new Rule(name, Type.BOOLEAN, null, opposite, new Default(active, null));
	}

	static Rule text(String name, List<String> options, boolean active, String value) {
		return new Rule(name, Type.TEXT, options, null, new Default(active, value));
	}

	static Rule number(String name, boolean active, int value) {
		return new Rule(name, Type.NUMBER, null, null, new Default(active, value));
	}
}
