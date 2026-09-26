/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import com.cyphail.ast.BooleanLiteral;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.CreateStatement;
import com.cyphail.ast.DeleteStatement;
import com.cyphail.ast.Expression;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.Statement;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Recorre el AST y lo escribe en el formato del SPEC de P1:
// bloques Query/Match/Where/Updates/Return, y las expresiones en pre-orden.
public final class AstPrinter {

    private static final String NL = System.lineSeparator();

    private AstPrinter() {
    }

    public static String print(Program program) {
        var matches = program.clauses().stream()
                .filter(MatchStatement.class::isInstance)
                .map(MatchStatement.class::cast)
                .toList();

        var updates = program.clauses().stream()
                .filter(clause -> !(clause instanceof MatchStatement))
                .toList();

        return "Query{" + NL
                + matchBlock(matches)
                + whereBlock(matches)
                + updatesBlock(updates)
                + returnBlock(program.returnItems())
                + "}" + NL;
    }

    // ---------- expresiones en pre-orden ----------

    // (> (. m year) 1990)
    public static String expression(Expression expression) {
        return switch (expression) {

            case NumberLiteral number -> String.valueOf(number.value());

            case StringLiteral string -> "\"" + string.value() + "\"";

            case BooleanLiteral bool -> String.valueOf(bool.value());

            case VariableExpression variable -> variable.name();

            case PropertyAccessExpression access ->
                    "(. %s %s)".formatted(access.variable(), access.property());

            case ComparisonExpression comparison ->
                    "(%s %s %s)".formatted(symbol(comparison.operator()),
                                           expression(comparison.left()),
                                           expression(comparison.right()));
        };
    }

    private static String symbol(ComparisonOperator operator) {
        return switch (operator) {
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case NOT_EQUALS -> "<>";
        };
    }

    // ---------- bloques ----------

    private static String matchBlock(List<MatchStatement> matches) {
        var patterns = matches.stream().flatMap(match -> match.patterns().stream()).toList();

        return indent(1) + "Match: {" + NL
                + indent(2) + "Patterns: [" + NL
                + patterns.stream().map(AstPrinter::patternNode).collect(Collectors.joining())
                + indent(2) + "]" + NL
                + indent(1) + "}" + NL;
    }

    private static String patternNode(NodePattern pattern) {
        var variable = pattern.variable()
                .map(name -> indent(4) + "var: " + name + NL)
                .orElse("");

        return indent(3) + "PatternNode:{" + NL
                + variable
                + indent(4) + "labels: " + list(pattern.labels()) + NL
                + indent(4) + "properties: " + properties(pattern.properties()) + NL
                + indent(3) + "}" + NL;
    }

    // [] o [ {year 1999}, {age 40} ]
    private static String properties(List<PropertyEntry> entries) {
        return list(entries.stream()
                .map(entry -> "{%s %s}".formatted(entry.name(), expression(entry.value())))
                .toList());
    }

    // El SPEC omite el bloque cuando no hay WHERE.
    private static String whereBlock(List<MatchStatement> matches) {
        return matches.stream()
                .map(MatchStatement::where)
                .flatMap(Optional::stream)
                .findFirst()
                .map(where -> indent(1) + "Where: {" + NL
                        + indent(2) + "Expr: " + expression(where) + NL
                        + indent(1) + "}" + NL)
                .orElse("");
    }

    private static String updatesBlock(List<Statement> updates) {
        if (updates.isEmpty()) {
            return indent(1) + "Updates:[]" + NL;
        }

        return indent(1) + "Updates:[" + NL
                + updates.stream().map(AstPrinter::update).collect(Collectors.joining())
                + indent(1) + "]" + NL;
    }

    private static String update(Statement statement) {
        return switch (statement) {

            case CreateStatement create -> indent(2) + "Create:{" + NL
                    + indent(3) + "Patterns: [" + NL
                    + create.patterns().stream()
                            .map(pattern -> shift(patternNode(pattern)))
                            .collect(Collectors.joining())
                    + indent(3) + "]" + NL
                    + indent(2) + "}" + NL;

            case DeleteStatement delete -> indent(2) + "Delete:{" + NL
                    + indent(3) + "Targets: "
                    + list(delete.targets().stream().map(AstPrinter::expression).toList()) + NL
                    + indent(2) + "}" + NL;

            // un MATCH nunca llega aqui: se filtro antes
            case MatchStatement match -> "";
        };
    }

    private static String returnBlock(List<ReturnItem> items) {
        return indent(1) + "Return:{" + NL
                + indent(2) + "Projection:{" + NL
                + indent(3) + "Items:[" + NL
                + items.stream().map(AstPrinter::item).collect(Collectors.joining())
                + indent(3) + "]" + NL
                + indent(3) + "Modifiers:[]" + NL
                + indent(2) + "}" + NL
                + indent(1) + "}" + NL;
    }

    // {as (. m title) title}  o  {(. m title)} si no hay alias
    private static String item(ReturnItem item) {
        var expression = expression(item.expression());

        return indent(4) + item.alias()
                .map(alias -> "{as %s %s}".formatted(expression, alias))
                .orElse("{%s}".formatted(expression)) + NL;
    }

    // ---------- ayudas ----------

    private static String list(List<String> values) {
        return values.isEmpty() ? "[]" : "[ " + String.join(", ", values) + " ]";
    }

    private static String indent(int level) {
        return "  ".repeat(level);
    }

    // Empuja un bloque ya formateado un nivel mas adentro.
    private static String shift(String block) {
        return block.lines().map(line -> "  " + line).collect(Collectors.joining(NL, "", NL));
    }
}
