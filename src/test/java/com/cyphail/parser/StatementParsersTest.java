/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.VariableExpression;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.TokenString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class StatementParsersTest {

    private static InputTokens tokens(String source) {
        var ok = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var list = (List<TokenString>) ok.token();
        return new InputTokens(list);
    }

    private static Program program(String source) {
        var ok = assertInstanceOf(Ok.class, StatementParsers.program().parse(tokens(source)));
        return (Program) ok.token();
    }

    // ---------- 5a. ReturnItem ----------

    @Test
    void returnItemWithoutAlias() {
        var ok = assertInstanceOf(Ok.class, StatementParsers.returnItem().parse(tokens("p.name")));

        assertEquals(new ReturnItem(new PropertyAccessExpression("p", "name"), Optional.empty()),
                ok.token());
    }

    @Test
    void returnItemWithAlias() {
        var ok = assertInstanceOf(Ok.class,
                StatementParsers.returnItem().parse(tokens("p.name AS nombre")));

        assertEquals(new ReturnItem(new PropertyAccessExpression("p", "name"),
                        Optional.of("nombre")),
                ok.token());
    }

    // ---------- 5b. MATCH ----------

    @Test
    void matchWithoutWhere() {
        var ok = assertInstanceOf(Ok.class,
                StatementParsers.matchStatement().parse(tokens("MATCH (p:Person)")));

        assertEquals(new MatchStatement(
                        List.of(new NodePattern(Optional.of("p"), List.of("Person"), List.of())),
                        Optional.empty()),
                ok.token());
    }

    @Test
    void matchWithSeveralPatterns() {
        var ok = assertInstanceOf(Ok.class,
                StatementParsers.matchStatement().parse(tokens("MATCH (a), (b)")));

        var match = (MatchStatement) ok.token();
        assertEquals(2, match.patterns().size());
    }

    @Test
    void matchWithWhereDropsTheKeyword() {
        var ok = assertInstanceOf(Ok.class,
                StatementParsers.matchStatement().parse(tokens("MATCH (p) WHERE p.age > 25")));

        var match = (MatchStatement) ok.token();
        assertEquals(Optional.of(new ComparisonExpression(
                        new PropertyAccessExpression("p", "age"),
                        ComparisonOperator.GREATER_THAN,
                        new NumberLiteral(25))),
                match.where());
    }

    // ---------- 5c. Casos del SPEC ----------

    @Test
    void caso1MatchSimple() {
        var p = program("MATCH (p:Person) RETURN p.name");

        assertEquals(new Program(
                        List.of(new MatchStatement(
                                List.of(new NodePattern(Optional.of("p"), List.of("Person"), List.of())),
                                Optional.empty())),
                        List.of(new ReturnItem(new PropertyAccessExpression("p", "name"),
                                Optional.empty()))),
                p);
    }

    @Test
    void caso3FiltroConWhere() {
        // tal como lo da el SPEC, con salto de linea
        var p = program("""
                MATCH (p:Person)
                WHERE p.age > 25
                RETURN p.name""");

        var match = (MatchStatement) p.clauses().get(0);
        assertEquals(Optional.of(new ComparisonExpression(
                        new PropertyAccessExpression("p", "age"),
                        ComparisonOperator.GREATER_THAN,
                        new NumberLiteral(25))),
                match.where());
        assertEquals(1, p.returnItems().size());
    }

    @Test
    void variosItemsEnElReturn() {
        var p = program("MATCH (a), (b) RETURN a, b AS segundo");

        assertEquals(List.of(new ReturnItem(new VariableExpression("a"), Optional.empty()),
                        new ReturnItem(new VariableExpression("b"), Optional.of("segundo"))),
                p.returnItems());
    }

    // ---------- 5c. Lo que debe fallar ----------

    @Test
    void matchSinReturnFalla() {
        assertInstanceOf(Fail.class, StatementParsers.program().parse(tokens("MATCH (p:Person)")));
    }

    @Test
    void basuraAlFinalFalla() {
        // sin el Token(EOF) final esto pasaria, dejando "basura" sin leer
        assertInstanceOf(Fail.class,
                StatementParsers.program().parse(tokens("MATCH (p) RETURN p basura")));
    }

    @Test
    void caso2NoSoportadoTodavia() {
        // MATCH (p:Person)-[:FOLLOWS]->(q:Person) necesita un patron de relacion.
        // El AST no tiene una clase para eso: MatchStatement solo guarda NodePattern.
        assertInstanceOf(Fail.class, StatementParsers.program().parse(
                tokens("MATCH (p:Person)-[:FOLLOWS]->(q:Person) RETURN p.name, q.name")));
    }
}
