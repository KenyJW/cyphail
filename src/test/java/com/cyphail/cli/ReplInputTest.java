/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplInputTest {

    @Test
    void aLineWithoutSemicolonStaysPending() {
        var feed = ReplInput.feed("", "MATCH (m:Movie)");

        assertEquals(List.of(), feed.statements());
        assertEquals("MATCH (m:Movie)", feed.pending());
        assertEquals("... ", ReplInput.prompt(feed.pending()));
    }

    @Test
    void theSecondLineCompletesTheStatement() {
        var feed = ReplInput.feed("MATCH (m:Movie)", "RETURN m.title;");

        assertEquals(List.of("MATCH (m:Movie)\nRETURN m.title"), feed.statements());
        assertTrue(feed.pending().isEmpty());
        assertEquals(">>> ", ReplInput.prompt(feed.pending()));
    }

    @Test
    void aDotCommandIsDispatchedOnItsOwn() {
        var feed = ReplInput.feed("", ".help");

        assertEquals(Optional.of(".help"), feed.command());
        assertEquals(List.of(), feed.statements());
        assertEquals(">>> ", ReplInput.prompt(feed.pending()));
    }

    @Test
    void aDotIsNotACommandWhenSomethingIsPending() {
        // "RETURN m" + ".title;" es una linea de continuacion, no un comando
        var feed = ReplInput.feed("MATCH (m:Movie) RETURN m", ".title;");

        assertTrue(feed.command().isEmpty());
        assertEquals(List.of("MATCH (m:Movie) RETURN m\n.title"), feed.statements());
    }

    @Test
    void exitWinsEvenWithSomethingPending() {
        // la unica excepcion: sin esto el usuario queda atrapado
        var feed = ReplInput.feed("MATCH (m:Movie)", ".exit");

        assertEquals(Optional.of(".exit"), feed.command());
        assertTrue(feed.pending().isEmpty());
    }

    @Test
    void exitIsCaseInsensitive() {
        assertEquals(Optional.of(".EXIT"), ReplInput.feed("MATCH (m)", ".EXIT").command());
    }

    @Test
    void anEmptyLineChangesNothing() {
        var feed = ReplInput.feed("MATCH (m:Movie)", "");

        assertEquals(List.of(), feed.statements());
        assertEquals("MATCH (m:Movie)", feed.pending());
    }
}
