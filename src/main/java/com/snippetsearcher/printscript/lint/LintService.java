package com.snippetsearcher.printscript.lint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.printscript.common.Version;
import org.printscript.config.RulesConfiguration;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runner.AnalyzingRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Lintea el código con las reglas activas que recibe (US-14, US-15).
@Service
public class LintService {

	private static final Logger log = LoggerFactory.getLogger(LintService.class);

	private final AnalyzingRunner runner = new AnalyzingRunner();

	public List<Diagnostic> lint(String code, Version version, Map<String, Object> rules) {
		// Falla con IllegalArgumentException si un valor no sirve: 400.
		var configuration = LinterConfiguration.from(new RulesConfiguration(rules));
		var diagnostics = new ArrayList<Diagnostic>();
		long start = System.nanoTime();
		runner.analyze(code.lines().iterator(), configuration, version,
				violation -> diagnostics.add(Diagnostic.from(violation)),
				failure -> diagnostics.add(Diagnostic.from(failure)));
		log.info("lintear {} con {} reglas: {} incumplidas en {} ms", version, rules.size(), diagnostics.size(),
				(System.nanoTime() - start) / 1_000_000);
		return diagnostics;
	}
}
