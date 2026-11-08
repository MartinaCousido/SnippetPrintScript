package com.snippetsearcher.printscript.lint;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.printscript.common.Version;
import org.printscript.config.RulesConfiguration;
import org.printscript.linter.config.LinterConfiguration;
import org.printscript.runner.AnalyzingRunner;
import org.springframework.stereotype.Service;

import com.snippetsearcher.printscript.diagnostic.Diagnostic;

// Lintea el código con las reglas activas que recibe (US-14, US-15).
@Service
public class LintService {

	private final AnalyzingRunner runner = new AnalyzingRunner();

	public List<Diagnostic> lint(String code, Version version, Map<String, Object> rules) {
		// Falla con IllegalArgumentException si un valor no sirve: 400.
		var configuration = LinterConfiguration.from(new RulesConfiguration(rules));
		var diagnostics = new ArrayList<Diagnostic>();
		runner.analyze(code.lines().iterator(), configuration, version,
				violation -> diagnostics.add(Diagnostic.from(violation)),
				failure -> diagnostics.add(Diagnostic.from(failure)));
		return diagnostics;
	}
}
