/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Code Generator)
 */
package com.cyphail.codegen;

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
import java.util.stream.Collectors;

public final class PrologCodeGenerator {

    private PrologCodeGenerator() {
    }

    public static String generate(Program program) {
        String clauses = join(
                program.clauses().stream()
                        .map(PrologCodeGenerator::statement)
                        .toList()
        );

        String returnItems = join(
                program.returnItems().stream()
                        .map(PrologCodeGenerator::returnItem)
                        .toList()
        );

        return "query([" + clauses + "], [" + returnItems + "]).";
    }

    private static String statement(Statement statement) {
        return switch (statement) {
            case MatchStatement match -> matchStatement(match);
            case CreateStatement create -> createStatement(create);
            case DeleteStatement delete -> deleteStatement(delete);
        };
    }

    private static String matchStatement(MatchStatement match) {
        String patterns = join(
                match.patterns().stream()
                        .map(PrologCodeGenerator::nodePattern)
                        .toList()
        );

        String where = match.where()
                .map(PrologCodeGenerator::expression)
                .orElse("none");

        return "match([" + patterns + "], " + where + ")";
    }

    private static String createStatement(CreateStatement create) {
        String patterns = join(
                create.patterns().stream()
                        .map(PrologCodeGenerator::nodePattern)
                        .toList()
        );

        return "create([" + patterns + "])";
    }

    private static String deleteStatement(DeleteStatement delete) {
        String targets = join(
                delete.targets().stream()
                        .map(PrologCodeGenerator::expression)
                        .toList()
        );

        return "delete([" + targets + "])";
    }

    private static String nodePattern(NodePattern node) {
        String variable = node.variable()
                .map(name -> "var(" + atom(name) + ")")
                .orElse("anonymous");

        String labels = join(
                node.labels().stream()
                        .map(PrologCodeGenerator::atom)
                        .toList()
        );

        String properties = join(
                node.properties().stream()
                        .map(PrologCodeGenerator::property)
                        .toList()
        );

        return "node("
                + variable
                + ", ["
                + labels
                + "], ["
                + properties
                + "])";
    }

    private static String property(PropertyEntry property) {
        return "property("
                + atom(property.name())
                + ", "
                + expression(property.value())
                + ")";
    }

    private static String returnItem(ReturnItem item) {
        String alias = item.alias()
                .map(PrologCodeGenerator::atom)
                .orElse("none");

        return "return_item("
                + expression(item.expression())
                + ", "
                + alias
                + ")";
    }

    private static String expression(Expression expression) {
        return switch (expression) {
            case NumberLiteral number ->
                    Long.toString(number.value());

            case StringLiteral string ->
                    atom(string.value());

            case BooleanLiteral bool ->
                    Boolean.toString(bool.value());

            case VariableExpression variable ->
                    "var(" + atom(variable.name()) + ")";

            case PropertyAccessExpression property ->
                    "property_access("
                            + atom(property.variable())
                            + ", "
                            + atom(property.property())
                            + ")";

            case ComparisonExpression comparison ->
                    "compare("
                            + expression(comparison.left())
                            + ", "
                            + comparisonOperator(comparison.operator())
                            + ", "
                            + expression(comparison.right())
                            + ")";
        };
    }

    private static String comparisonOperator(
            ComparisonOperator operator
    ) {
        return switch (operator) {
            case LESS_THAN -> "'<'";
            case GREATER_THAN -> "'>'";
            case NOT_EQUALS -> "'<>'";
        };
    }

    private static String atom(String value) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("'", "\\'");

        return "'" + escaped + "'";
    }

    private static String join(List<String> values) {
        return values.stream()
                .collect(Collectors.joining(", "));
    }
}