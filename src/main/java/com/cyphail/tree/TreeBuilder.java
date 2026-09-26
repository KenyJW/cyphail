/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import com.cyphail.ast.BooleanLiteral;
import com.cyphail.ast.ComparisonExpression;
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
import java.util.stream.Stream;

// FASE A: recorre el AST y lo convierte en TreeNode. No imprime nada.
// Lo ausente se omite: un patron sin variable no lleva nodo "variable".
public final class TreeBuilder {

    private TreeBuilder() {
    }

    public static TreeNode of(Program program) {
        var clauses = program.clauses().stream().map(TreeBuilder::clause);

        var returnPart = program.returnItems().isEmpty()
                ? Stream.<TreeNode>of()
                : Stream.of(TreeNode.node("returnPart",
                        program.returnItems().stream().map(TreeBuilder::returnItem).toList()));

        return TreeNode.node("Query", Stream.concat(clauses, returnPart).toList());
    }

    private static TreeNode clause(Statement statement) {
        return switch (statement) {

            case MatchStatement match -> TreeNode.node("matchPart",
                    Stream.concat(
                            match.patterns().stream().map(TreeBuilder::nodePattern),
                            match.where().map(w -> TreeNode.node("where", expression(w))).stream())
                    .toList());

            case CreateStatement create -> TreeNode.node("createPart",
                    create.patterns().stream().map(TreeBuilder::nodePattern).toList());

            case DeleteStatement delete -> TreeNode.node("deletePart",
                    delete.targets().stream().map(TreeBuilder::expression).toList());
        };
    }

    private static TreeNode nodePattern(NodePattern pattern) {
        var variable = pattern.variable()
                .map(name -> TreeNode.field("variable", name))
                .stream();

        var labels = pattern.labels().isEmpty()
                ? Stream.<TreeNode>of()
                : Stream.of(TreeNode.node("labels",
                        pattern.labels().stream().map(TreeNode::leaf).toList()));

        var properties = pattern.properties().isEmpty()
                ? Stream.<TreeNode>of()
                : Stream.of(TreeNode.node("properties",
                        pattern.properties().stream().map(TreeBuilder::property).toList()));

        return TreeNode.node("nodePattern",
                Stream.of(variable, labels, properties).flatMap(s -> s).toList());
    }

    private static TreeNode property(PropertyEntry entry) {
        return TreeNode.node("property",
                TreeNode.field("key", entry.name()),
                TreeNode.node("value", expression(entry.value())));
    }

    private static TreeNode returnItem(ReturnItem item) {
        var alias = item.alias().map(name -> TreeNode.field("alias", name)).stream();

        return TreeNode.node("returnItem",
                Stream.concat(Stream.of(expression(item.expression())), alias).toList());
    }

    private static TreeNode expression(Expression expression) {
        return switch (expression) {

            case NumberLiteral number -> TreeNode.field("number", String.valueOf(number.value()));

            case StringLiteral string -> TreeNode.field("string", string.value());

            case BooleanLiteral bool -> TreeNode.field("boolean", String.valueOf(bool.value()));

            case VariableExpression variable -> TreeNode.field("variable", variable.name());

            case PropertyAccessExpression access -> TreeNode.node("propertyAccess",
                    TreeNode.field("variable", access.variable()),
                    TreeNode.field("property", access.property()));

            case ComparisonExpression comparison -> TreeNode.node("comparison",
                    TreeNode.node("left", expression(comparison.left())),
                    TreeNode.field("operator", comparison.operator().name()),
                    TreeNode.node("right", expression(comparison.right())));
        };
    }
}
