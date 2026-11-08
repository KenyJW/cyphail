/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Code Generator)
 */
package com.cyphail.codegen;

import com.cyphail.ast.AndExpression;
import com.cyphail.ast.BooleanLiteral;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.CreateStatement;
import com.cyphail.ast.DecimalLiteral;
import com.cyphail.ast.DeleteStatement;
import com.cyphail.ast.Direction;
import com.cyphail.ast.Expression;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NotExpression;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.OrExpression;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.PatternStep;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.RelationshipPattern;
import com.cyphail.ast.RemoveStatement;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.SetItem;
import com.cyphail.ast.SetStatement;
import com.cyphail.ast.Statement;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;
import com.cyphail.ast.VariableTable;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class PrologCodeGenerator {

    private PrologCodeGenerator() {
    }

    public static String generate(Program program) {
        Objects.requireNonNull(
                program,
                "program cannot be null"
        );

        PrologVariableTable variables =
                PrologVariableTable.from(
                        VariableTable.from(program)
                );

        String clauses = join(
                program.clauses().stream()
                        .map(statement ->
                                statement(
                                        statement,
                                        variables
                                )
                        )
                        .toList()
        );

        String returnItems = join(
                program.returnItems().stream()
                        .map(item ->
                                returnItem(
                                        item,
                                        variables
                                )
                        )
                        .toList()
        );

        return "ensure_loaded("
                + atom("prolog/cyphail_engine.pl")
                + "), "
                + "cy_execute("
                + "query(["
                + clauses
                + "], ["
                + returnItems
                + "]), "
                + PrologVariableTable.ROWS
                + ").";
    }

    private static String statement(
            Statement statement,
            PrologVariableTable variables
    ) {
        return switch (statement) {
            case MatchStatement match ->
                    matchStatement(match, variables);

            case CreateStatement create ->
                    createStatement(create, variables);

            case DeleteStatement delete ->
                    deleteStatement(delete, variables);

            case SetStatement set ->
                    setStatement(set, variables);

            case RemoveStatement remove ->
                    removeStatement(remove, variables);
        };
    }

    private static String matchStatement(
            MatchStatement match,
            PrologVariableTable variables
    ) {
        String patterns = join(
                match.patterns().stream()
                        .map(path ->
                                pathPattern(
                                        path,
                                        variables
                                )
                        )
                        .toList()
        );

        String where = match.where()
                .map(expression ->
                        expression(
                                expression,
                                variables
                        )
                )
                .orElse("none");

        return "match(["
                + patterns
                + "], "
                + where
                + ")";
    }

    private static String createStatement(
            CreateStatement create,
            PrologVariableTable variables
    ) {
        String patterns = join(
                create.patterns().stream()
                        .map(path ->
                                pathPattern(
                                        path,
                                        variables
                                )
                        )
                        .toList()
        );

        return "create(["
                + patterns
                + "])";
    }

    private static String deleteStatement(
            DeleteStatement delete,
            PrologVariableTable variables
    ) {
        String targets = join(
                delete.targets().stream()
                        .map(target ->
                                expression(
                                        target,
                                        variables
                                )
                        )
                        .toList()
        );

        return "delete(["
                + targets
                + "])";
    }

    private static String setStatement(
            SetStatement set,
            PrologVariableTable variables
    ) {
        String items = join(
                set.items().stream()
                        .map(item ->
                                setItem(
                                        item,
                                        variables
                                )
                        )
                        .toList()
        );

        return "set(["
                + items
                + "])";
    }

    private static String setItem(
            SetItem item,
            PrologVariableTable variables
    ) {
        return "set_item("
                + expression(
                item.target(),
                variables
        )
                + ", "
                + expression(
                item.value(),
                variables
        )
                + ")";
    }

    private static String removeStatement(
            RemoveStatement remove,
            PrologVariableTable variables
    ) {
        String targets = join(
                remove.targets().stream()
                        .map(target ->
                                expression(
                                        target,
                                        variables
                                )
                        )
                        .toList()
        );

        return "remove(["
                + targets
                + "])";
    }

    private static String pathPattern(
            PathPattern path,
            PrologVariableTable variables
    ) {
        if (path.steps().isEmpty()) {
            return nodePattern(
                    path.start(),
                    variables
            );
        }

        String steps = join(
                path.steps().stream()
                        .map(step ->
                                patternStep(
                                        step,
                                        variables
                                )
                        )
                        .toList()
        );

        return "path("
                + nodePattern(
                path.start(),
                variables
        )
                + ", ["
                + steps
                + "])";
    }

    private static String patternStep(
            PatternStep step,
            PrologVariableTable variables
    ) {
        return "step("
                + relationshipPattern(
                step.relationship(),
                variables
        )
                + ", "
                + nodePattern(
                step.node(),
                variables
        )
                + ")";
    }

    private static String relationshipPattern(
            RelationshipPattern relationship,
            PrologVariableTable variables
    ) {
        String variable = relationship.variable()
                .map(variables::variable)
                .orElse("anonymous");

        String type = relationship.type()
                .map(PrologCodeGenerator::atom)
                .orElse("none");

        String properties = join(
                relationship.properties().stream()
                        .map(property ->
                                property(
                                        property,
                                        variables
                                )
                        )
                        .toList()
        );

        return "relationship("
                + variable
                + ", "
                + type
                + ", ["
                + properties
                + "], "
                + direction(relationship.direction())
                + ")";
    }

    private static String direction(
            Direction direction
    ) {
        return switch (direction) {
            case OUT -> "out";
            case IN -> "in";
            case NONE -> "none";
        };
    }

    private static String nodePattern(
            NodePattern node,
            PrologVariableTable variables
    ) {
        String variable = node.variable()
                .map(variables::variable)
                .orElse("anonymous");

        String labels = join(
                node.labels().stream()
                        .map(PrologCodeGenerator::atom)
                        .toList()
        );

        String properties = join(
                node.properties().stream()
                        .map(property ->
                                property(
                                        property,
                                        variables
                                )
                        )
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

    private static String property(
            PropertyEntry property,
            PrologVariableTable variables
    ) {
        return "property("
                + atom(property.name())
                + ", "
                + expression(
                property.value(),
                variables
        )
                + ")";
    }

    private static String returnItem(
            ReturnItem item,
            PrologVariableTable variables
    ) {
        String alias = item.alias()
                .map(PrologCodeGenerator::atom)
                .orElseGet(
                        () -> defaultReturnHeader(
                                item.expression()
                        )
                );

        return "return_item("
                + expression(
                item.expression(),
                variables
        )
                + ", "
                + alias
                + ")";
    }

    private static String defaultReturnHeader(
            Expression expression
    ) {
        if (expression instanceof PropertyAccessExpression property) {
            return atom(
                    property.variable()
                            + "."
                            + property.property()
            );
        }

        if (expression instanceof VariableExpression variable) {
            return atom(variable.name());
        }

        return "none";
    }

    private static String expression(
            Expression expression,
            PrologVariableTable variables
    ) {
        return switch (expression) {
            case DecimalLiteral decimal ->
                    Double.toString(decimal.value());

            case AndExpression and ->
                    "and("
                            + expression(
                            and.left(),
                            variables
                    )
                            + ", "
                            + expression(
                            and.right(),
                            variables
                    )
                            + ")";

            case NotExpression not ->
                    "not("
                            + expression(
                            not.operand(),
                            variables
                    )
                            + ")";

            case OrExpression or ->
                    "or("
                            + expression(
                            or.left(),
                            variables
                    )
                            + ", "
                            + expression(
                            or.right(),
                            variables
                    )
                            + ")";

            case NumberLiteral number ->
                    Long.toString(number.value());

            case StringLiteral string ->
                    atom(string.value());

            case BooleanLiteral bool ->
                    Boolean.toString(bool.value());

            case VariableExpression variable ->
                    variables.variable(
                            variable.name()
                    );

            case PropertyAccessExpression property ->
                    "property_access("
                            + variables.variable(
                            property.variable()
                    )
                            + ", "
                            + atom(property.property())
                            + ", "
                            + variables.property(
                            property.variable(),
                            property.property()
                    )
                            + ")";

            case ComparisonExpression comparison ->
                    "compare("
                            + expression(
                            comparison.left(),
                            variables
                    )
                            + ", "
                            + comparisonOperator(
                            comparison.operator()
                    )
                            + ", "
                            + expression(
                            comparison.right(),
                            variables
                    )
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
            case EQUALS -> "'='";
            case LESS_OR_EQUAL -> "'<='";
            case GREATER_OR_EQUAL -> "'>='";
        };
    }

    private static String atom(
            String value
    ) {
        String escaped = value
                .replace("\\", "\\\\")
                .replace("'", "\\'");

        return "'"
                + escaped
                + "'";
    }

    private static String join(
            List<String> values
    ) {
        return values.stream()
                .collect(Collectors.joining(", "));
    }
}