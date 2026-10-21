/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.Direction;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.RelationshipPattern;
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

    // ---------- 4d. Patron de relacion (P2) ----------

    @Test
    void relationshipWithTypeOnlyPointsOut() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.relationshipPattern().parse(tokens("-[:amigo]->")));

        assertEquals(new RelationshipPattern(Optional.empty(), Optional.of("amigo"),
                        List.of(), Direction.OUT),
                ok.token());
    }

    @Test
    void relationshipWithVariableTypeAndProperties() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.relationshipPattern().parse(tokens("-[r:T {rol: \"Dev\"}]->")));

        assertEquals(new RelationshipPattern(Optional.of("r"), Optional.of("T"),
                        List.of(new PropertyEntry("rol", new StringLiteral("Dev"))),
                        Direction.OUT),
                ok.token());
    }

    @Test
    void relationshipCanPointIn() {
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.relationshipPattern().parse(tokens("<-[:T]-")));

        assertEquals(new RelationshipPattern(Optional.empty(), Optional.of("T"),
                        List.of(), Direction.IN),
                ok.token());
    }

    @Test
    void relationshipWithoutArrowsHasNoDirection() {
        // el SPEC lo usa: -[r]- sin tipo y sin flecha
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.relationshipPattern().parse(tokens("-[r]-")));

        assertEquals(new RelationshipPattern(Optional.of("r"), Optional.empty(),
                        List.of(), Direction.NONE),
                ok.token());
    }

    @Test
    void theFailedAlternativeLeavesTheInputIntact() {
        // OUT se prueba primero y falla al esperar "->" despues de consumir
        // -[:T]. Como un fallo no consume nada, NONE arranca de cero y acierta.
        var ok = assertInstanceOf(Ok.class,
                PatternParsers.relationshipPattern().parse(tokens("-[:T]-")));

        assertEquals(Direction.NONE, ((RelationshipPattern) ok.token()).direction());
    }

    @Test
    void relationshipFailsWithoutClosingBracket() {
        assertInstanceOf(Fail.class, PatternParsers.relationshipPattern().parse(tokens("-[r:T->")));
    }

    // ---------- 4e. Camino completo (P2) ----------

    private static PathPattern path(String source) {
        var ok = assertInstanceOf(Ok.class, PatternParsers.pathPattern().parse(tokens(source)));
        return (PathPattern) ok.token();
    }

    @Test
    void aLoneNodeIsAPathWithoutSteps() {
        var camino = path("(a)");

        assertEquals(Optional.of("a"), camino.start().variable());
        assertEquals(List.of(), camino.steps());
    }

    @Test
    void pathWithOneStep() {
        var camino = path("(a)-[:R]->(b)");

        assertEquals(Optional.of("a"), camino.start().variable());
        assertEquals(1, camino.steps().size());
        assertEquals(Direction.OUT, camino.steps().get(0).relationship().direction());
        assertEquals(Optional.of("R"), camino.steps().get(0).relationship().type());
        assertEquals(Optional.of("b"), camino.steps().get(0).node().variable());
    }

    @Test
    void pathWithTwoStepsKeepsEachDirection() {
        var camino = path("(a)-[:R]->(b)<-[:S]-(c)");

        assertEquals(2, camino.steps().size());
        assertEquals(Direction.OUT, camino.steps().get(0).relationship().direction());
        assertEquals(Direction.IN, camino.steps().get(1).relationship().direction());
        assertEquals(Optional.of("b"), camino.steps().get(0).node().variable());
        assertEquals(Optional.of("c"), camino.steps().get(1).node().variable());
    }

    @Test
    void pathKeepsLabelsAndPropertiesOfItsNodes() {
        var camino = path("(p:Persona {id: 1})-[r:TRABAJA_EN]->(e:Empresa)");

        assertEquals(List.of("Persona"), camino.start().labels());
        assertEquals(List.of(new PropertyEntry("id", new NumberLiteral(1))),
                camino.start().properties());
        assertEquals(Optional.of("r"), camino.steps().get(0).relationship().variable());
        assertEquals(List.of("Empresa"), camino.steps().get(0).node().labels());
    }

    @Test
    void pathStopsBeforeAStepWithoutArrivalNode() {
        // la relacion sin nodo de llegada: el Star para, y queda "-[:R]->" sin leer
        var ok = assertInstanceOf(Ok.class, PatternParsers.pathPattern().parse(tokens("(a)-[:R]->")));

        assertEquals(List.of(), ((PathPattern) ok.token()).steps());
        assertEquals(3, ((InputTokens) ok.rest()).index());   // solo consumio ( a ), y dejo -[:R]->
    }
}
