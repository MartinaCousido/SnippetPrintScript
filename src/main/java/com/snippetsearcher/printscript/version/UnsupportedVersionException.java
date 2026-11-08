package com.snippetsearcher.printscript.version;

public class UnsupportedVersionException extends RuntimeException {

	public UnsupportedVersionException(String version) {
		super("Versión de PrintScript no soportada: " + version);
	}
}
