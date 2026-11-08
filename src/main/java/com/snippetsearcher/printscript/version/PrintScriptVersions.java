package com.snippetsearcher.printscript.version;

import java.util.Map;

import org.printscript.common.Version;

// Traduce la versión que manda Snippet Service ("1.0", "1.1") a la de la librería.
public final class PrintScriptVersions {

	private static final Map<String, Version> VERSIONS = Map.of(
			"1.0", Version.V10,
			"1.1", Version.V11);

	private PrintScriptVersions() {
	}

	public static Version parse(String version) {
		Version parsed = VERSIONS.get(version);
		if (parsed == null) {
			throw new UnsupportedVersionException(version);
		}
		return parsed;
	}
}
