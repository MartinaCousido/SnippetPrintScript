package com.snippetsearcher.printscript.format;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Map;

import org.printscript.common.Version;
import org.printscript.config.RulesConfiguration;
import org.printscript.formatter.config.FormatterConfiguration;
import org.printscript.runner.FormattingRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Formatea el código con las reglas activas que recibe (US-11, US-12).
@Service
public class FormatService {

	private static final Logger log = LoggerFactory.getLogger(FormatService.class);

	private final FormattingRunner runner = new FormattingRunner();

	public FormatResponse format(String code, Version version, Map<String, Object> rules) {
		// Falla con IllegalArgumentException si un valor no sirve: 400.
		var configuration = FormatterConfiguration.from(new RulesConfiguration(rules));
		var formatted = new StringWriter();
		var diagnostics = new ArrayList<Diagnostic>();
		long start = System.nanoTime();
		runner.format(code.lines().iterator(), configuration, formatted, version,
				failure -> diagnostics.add(Diagnostic.from(failure)));
		log.info("formatear {} con {} reglas: {} errores en {} ms", version, rules.size(), diagnostics.size(),
				(System.nanoTime() - start) / 1_000_000);
		// Un formateo a medias no sirve: si falló, el código queda como estaba.
		return new FormatResponse(diagnostics.isEmpty() ? formatted.toString() : code, diagnostics);
	}
}
