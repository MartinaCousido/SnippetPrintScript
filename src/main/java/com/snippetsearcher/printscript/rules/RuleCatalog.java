package com.snippetsearcher.printscript.rules;

import static org.printscript.formatter.config.FormatterConfiguration.IF_BRACE_BELOW_LINE_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.IF_BRACE_SAME_LINE_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.INDENT_INSIDE_IF_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.LINE_BREAKS_AFTER_PRINTLN_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.LINE_BREAK_AFTER_STATEMENT_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.NO_SPACING_AROUND_EQUALS_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.SINGLE_SPACE_SEPARATION_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.SPACE_SURROUNDING_OPERATIONS_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.SPACING_AFTER_COLON_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.SPACING_AROUND_EQUALS_KEY;
import static org.printscript.formatter.config.FormatterConfiguration.SPACING_BEFORE_COLON_KEY;
import static org.printscript.linter.config.LinterConfiguration.IDENTIFIER_FORMAT_KEY;
import static org.printscript.linter.config.LinterConfiguration.PRINTLN_ARGUMENT_KEY;
import static org.printscript.linter.config.LinterConfiguration.READ_INPUT_ARGUMENT_KEY;

import java.util.List;

import org.springframework.stereotype.Component;

// Las reglas de PrintScript, con sus valores por defecto. Los nombres son los
// de la librería. La librería no trae valores por defecto: los decide el
// proyecto (Decisiones técnicas de SnippetSearcher).
@Component
public class RuleCatalog {

	private static final List<Rule> LINT = List.of(
			Rule.text(IDENTIFIER_FORMAT_KEY, List.of("camel case", "snake case"), true, "camel case"),
			Rule.bool(PRINTLN_ARGUMENT_KEY, true),
			Rule.bool(READ_INPUT_ARGUMENT_KEY, true));

	private static final List<Rule> FORMAT = List.of(
			Rule.bool(SPACING_BEFORE_COLON_KEY, false),
			Rule.bool(SPACING_AFTER_COLON_KEY, true),
			Rule.opposite(SPACING_AROUND_EQUALS_KEY, NO_SPACING_AROUND_EQUALS_KEY, true),
			Rule.opposite(NO_SPACING_AROUND_EQUALS_KEY, SPACING_AROUND_EQUALS_KEY, false),
			Rule.bool(SPACE_SURROUNDING_OPERATIONS_KEY, true),
			// Pone un espacio entre todas las partes, incluso antes de ':' y dentro
			// de los paréntesis: arranca desactivada.
			Rule.bool(SINGLE_SPACE_SEPARATION_KEY, false),
			Rule.bool(LINE_BREAK_AFTER_STATEMENT_KEY, true),
			Rule.number(LINE_BREAKS_AFTER_PRINTLN_KEY, true, 0),
			Rule.opposite(IF_BRACE_SAME_LINE_KEY, IF_BRACE_BELOW_LINE_KEY, true),
			Rule.opposite(IF_BRACE_BELOW_LINE_KEY, IF_BRACE_SAME_LINE_KEY, false),
			Rule.number(INDENT_INSIDE_IF_KEY, true, 4));

	public List<Rule> lint() {
		return LINT;
	}

	public List<Rule> format() {
		return FORMAT;
	}
}
