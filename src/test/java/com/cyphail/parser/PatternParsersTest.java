/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;
import com.cyphail.lexer.TokenString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class PatternParsersTest {

    private static InputTokens tokens(String source) {
        var ok = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var list = (List<TokenString>) ok.token();
        return new InputTokens(list);
    }

    // ---------- 4b. Propiedades ----------

    @Test
    void propertyEntryBuildsNameAndValue() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.propertyEntry().parse(tokens("name: \"Ana\"")));

        assertEquals(new PropertyEntry("name", new StringLiteral("Ana")), ok.token());
    }

    @Test
    void propertyMapWithTwoEntries() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.propertyMap().parse(tokens("{name: \"Ana\", age: 30}")));

        assertEquals(List.of(new PropertyEntry("name", new StringLiteral("Ana")),
                        new PropertyEntry("age", new NumberLiteral(30))),
                ok.token());
    }

    @Test
    void propertyMapConsumesTheClosingBrace() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.propertyMap().parse(tokens("{age: 30}")));

        assertEquals(5, ((InputTokens) ok.rest()).index());   // { age : 30 }
    }

    @Test
    void propertyMapFailsWithoutClosingBrace() {
        assertInstanceOf(Fail.class, PatternParsers.propertyMap().parse(tokens("{age: 30")));
    }

    @Test
    void emptyPropertyMapIsNotSupportedYet() {
        // limitacion conocida: SepBy exige al menos un elemento
        assertInstanceOf(Fail.class, PatternParsers.propertyMap().parse(tokens("{}")));
    }

    // ---------- 4c. Node pattern ----------

    @Test
    void nodePatternWithVariableAndLabel() {
        var ok = assertInstanceOf(Ok.class, PatternParsers.nodePattern().parse(tokens("(p:Person)")));

        assertEquals(new NodePattern(Optional.of("p"), List.of("Person"), List.of()), ok.token());
    }

    @Test
    void nodePatternWithVariableOnly() {
        var ok = assertInstanceOf(Ok.class, PatternParsers.nodePattern().parse(tokens("(a)")));

        assertEquals(new NodePattern(Optional.of("a"), List.of(), List.of()), ok.token());
    }

    @Test
    void emptyNodePattern() {
        var ok = assertInstanceOf(Ok.class, PatternParsers.nodePattern().parse(tokens("()")));

        assertEquals(new NodePattern(Optional.empty(), List.of(), List.of()), ok.token());
    }

    @Test
    void nodePatternWithLabelOnly() {
        var ok = assertInstanceOf(Ok.class, PatternParsers.nodePattern().parse(tokens("(:Person)")));

        assertEquals(new NodePattern(Optional.empty(), List.of("Person"), List.of()), ok.token());
    }

    @Test
    void nodePatternAccumulatesSeveralLabels() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.nodePattern().parse(tokens("(a:Person:Admin)")));

        assertEquals(new NodePattern(Optional.of("a"), List.of("Person", "Admin"), List.of()),
                ok.token());
    }

    @Test
    void nodePatternWithProperties() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.nodePattern().parse(tokens("(a:Person {name: \"Ana\", age: 30})")));

        assertEquals(new NodePattern(Optional.of("a"), List.of("Person"),
                        List.of(new PropertyEntry("name", new StringLiteral("Ana")),
                                new PropertyEntry("age", new NumberLiteral(30)))),
                ok.token());
    }

    @Test
    void nodePatternFailsWithoutClosingParen() {
        assertInstanceOf(Fail.class, PatternParsers.nodePattern().parse(tokens("(p")));
    }
}
