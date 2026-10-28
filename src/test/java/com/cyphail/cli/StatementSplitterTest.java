/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatementSplitterTest {

    @Test
    void oneStatementLeavesNothingPending() {
        var split = StatementSplitter.split("MATCH (p) RETURN p;");

        assertEquals(List.of("MATCH (p) RETURN p"), split.statements());
        assertTrue(split.rest().isEmpty());
    }

    @Test
    void twoStatementsInTheSameText() {
        var split = StatementSplitter.split("MATCH (p) RETURN p; CREATE (a:Archive);");

        assertEquals(List.of("MATCH (p) RETURN p", "CREATE (a:Archive)"), split.statements());
        assertTrue(split.rest().isEmpty());
    }

    @Test
    void aStatementWithoutSemicolonStaysPending() {
        var split = StatementSplitter.split("MATCH (p) RETURN p");

        assertEquals(List.of(), split.statements());
        assertEquals("MATCH (p) RETURN p", split.rest());
    }

    @Test
    void aQueryCanSpanSeveralLines() {
        var split = StatementSplitter.split("""
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title AS title;""");

        assertEquals(1, split.statements().size());
        assertTrue(split.statements().get(0).contains("MATCH (m:Movie)"));
        assertTrue(split.statements().get(0).contains("RETURN m.title AS title"));
        assertTrue(split.rest().isEmpty());
    }

    // ---------- el ';' dentro de una cadena no corta ----------

    @Test
    void aSemicolonInsideSingleQuotesDoesNotCut() {
        var split = StatementSplitter.split("CREATE (a {nombre: 'Marta;Ana'});");

        assertEquals(List.of("CREATE (a {nombre: 'Marta;Ana'})"), split.statements());
    }

    @Test
    void aSemicolonInsideDoubleQuotesDoesNotCut() {
        var split = StatementSplitter.split("CREATE (a {nombre: \"Marta;Ana\"});");

        assertEquals(List.of("CREATE (a {nombre: \"Marta;Ana\"})"), split.statements());
    }

    @Test
    void onlyTheOpeningQuoteCloses() {
        // "O'Brien": la comilla simple de adentro no abre una cadena nueva,
        // asi que el ';' de afuera si corta
        var split = StatementSplitter.split("CREATE (a {nombre: \"O'Brien\"});");

        assertEquals(List.of("CREATE (a {nombre: \"O'Brien\"})"), split.statements());
        assertTrue(split.rest().isEmpty());
    }

    @Test
    void anUnclosedStringKeepsEverythingPending() {
        // sin cerrar la comilla, el ';' sigue siendo parte de la cadena
        var split = StatementSplitter.split("CREATE (a {nombre: 'Marta;");

        assertEquals(List.of(), split.statements());
        assertEquals("CREATE (a {nombre: 'Marta;", split.rest());
    }

    // ---------- bordes ----------

    @Test
    void emptyTextGivesNothing() {
        var split = StatementSplitter.split("");

        assertEquals(List.of(), split.statements());
        assertTrue(split.rest().isEmpty());
    }

    @Test
    void emptyStatementsAreDiscarded() {
        var split = StatementSplitter.split(";;  ;MATCH (p) RETURN p;");

        assertEquals(List.of("MATCH (p) RETURN p"), split.statements());
    }

    @Test
    void whatFollowsTheLastSemicolonIsPending() {
        var split = StatementSplitter.split("MATCH (p) RETURN p; MATCH (q)");

        assertEquals(List.of("MATCH (p) RETURN p"), split.statements());
        assertEquals("MATCH (q)", split.rest());
    }
}
