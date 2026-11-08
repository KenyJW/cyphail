/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    // ---------- lineas de comentario ----------

    @Test
    void aCommentLineIsIgnored() {
        // los casos del profesor traen un comentario encima; antes se pegaba
        // al estatuto siguiente y lo rompia con "Unrecognized character '/'"
        var feed = ReplInput.feed("", "// Caso 1 de la guia");

        assertEquals(List.of(), feed.statements());
        assertTrue(feed.pending().isEmpty());
        assertTrue(feed.command().isEmpty());
        assertEquals(">>> ", ReplInput.prompt(feed.pending()));
    }

    @Test
    void aCommentInsideAStatementLeavesThePendingIntact() {
        var feed = ReplInput.feed("MATCH (m:Movie)", "  // una nota");

        assertEquals("MATCH (m:Movie)", feed.pending());
        assertEquals(List.of(), feed.statements());
    }

    @Test
    void aUrlInsideAStringIsNotAComment() {
        // el // va dentro de la cadena, no al inicio de linea
        var feed = ReplInput.feed("", "CREATE (s {url: \"http://x.com\"});");

        assertEquals(List.of("CREATE (s {url: \"http://x.com\"})"), feed.statements());
    }

    @Test
    void theReplAndLoadAgreeOnWhatACommentIs() {
        assertTrue(ReplInput.isComment("// algo"));
        assertTrue(ReplInput.isComment("   // indentado"));
        assertFalse(ReplInput.isComment("MATCH (p) // al final"));
        assertFalse(ReplInput.isComment("CREATE (s {url: \"http://x\"})"));
    }
}
