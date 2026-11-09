package com.snippetsearcher.printscript.version;

import org.printscript.runner.ExecutionRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

// GET /version: la versión de la librería PrintScript que usa este servicio.
// Se lee del manifest del jar (Implementation-Version, desde la 0.3.2), no se
// escribe a mano. Snippet Service la pregunta cada 10 minutos y, si cambió,
// vuelve a validar todos los snippets (ver "Re-validar con cada versión del
// parser" en Decisiones técnicas de SnippetSearcher).
@RestController
public class VersionController {

	public record VersionResponse(String library) {
	}

	@GetMapping("/version")
	public VersionResponse version() {
		return new VersionResponse(ExecutionRunner.class.getPackage().getImplementationVersion());
	}
}
