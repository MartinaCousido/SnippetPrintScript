package com.snippetsearcher.printscript.version;

public class UnsupportedVersionException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public UnsupportedVersionException(String version) {
		super("Versión de PrintScript no soportada: " + version);
	}
}
