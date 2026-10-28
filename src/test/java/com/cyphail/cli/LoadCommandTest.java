/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import com.cyphail.frontend.CyphailResponse;
import com.cyphail.frontend.RequestHandler;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoadCommandTest {

    @TempDir
    Path folder;

    private final List<String> received = new ArrayList<>();

    private final RequestHandler handler = statement -> {
        received.add(statement);
        return CyphailResponse.ok("ok: " + statement);
    };

    private String file(String content) throws IOException {
        var path = folder.resolve("script.cyphail");
        Files.writeString(path, content);
        return path.toString();
    }

    @Test
    void runsEveryStatementOfTheFile() throws IOException {
        var output = LoadCommand.run(file("MATCH (p) RETURN p; CREATE (a:Archive);"), handler);

        assertEquals(List.of("MATCH (p) RETURN p", "CREATE (a:Archive)"), received);
        assertTrue(output.contains("ok: MATCH (p) RETURN p"));
        assertTrue(output.contains("ok: CREATE (a:Archive)"));
    }

    @Test
    void aStatementCanSpanSeveralLines() throws IOException {
        LoadCommand.run(file("""
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title;"""), handler);

        assertEquals(1, received.size());
        assertTrue(received.get(0).contains("MATCH (m:Movie)"));
        assertTrue(received.get(0).contains("RETURN m.title"));
    }

    @Test
    void commentLinesAreIgnored() throws IOException {
        LoadCommand.run(file("// demo\nMATCH (p) RETURN p;\n// otro\n"), handler);

        assertEquals(List.of("MATCH (p) RETURN p"), received);
    }

    @Test
    void aUrlInsideAStringIsNotACommentLine() throws IOException {
        // el // va dentro de la cadena, no al inicio de linea
        LoadCommand.run(file("CREATE (s {url: \"http://x.com\"});"), handler);

        assertEquals(List.of("CREATE (s {url: \"http://x.com\"})"), received);
    }

    // ---------- la ruta ----------

    @Test
    void thePathCanComeInQuotes() throws IOException {
        // el SPEC lo escribe asi: .load "somepath/mi_script.cyphail"
        var output = LoadCommand.run("\"" + file("MATCH (p) RETURN p;") + "\"", handler);

        assertEquals(List.of("MATCH (p) RETURN p"), received);
        assertTrue(output.contains("ok:"));
    }

    @Test
    void withoutAPathItSaysSo() {
        assertEquals("ERROR: .load needs a file path", LoadCommand.run("", handler));
    }

    @Test
    void aMissingFileIsReported() {
        var output = LoadCommand.run(folder.resolve("nope.cyphail").toString(), handler);

        assertTrue(output.startsWith("ERROR: cannot read"));
        assertTrue(received.isEmpty());
    }

    // ---------- bordes ----------

    @Test
    void anIncompleteLastStatementIsNotRun() throws IOException {
        var output = LoadCommand.run(file("MATCH (p) RETURN p; MATCH (q)"), handler);

        assertEquals(List.of("MATCH (p) RETURN p"), received);
        assertTrue(output.contains("incomplete statement at the end of the file"));
    }

    @Test
    void anEmptyFileSaysThereIsNothingToRun() throws IOException {
        assertEquals("ERROR: no statements found", LoadCommand.run(file("// solo un comentario\n"), handler));
    }

    @Test
    void aFailedStatementIsPrefixedWithError() throws IOException {
        var output = LoadCommand.run(file("MATCH broken;"),
                statement -> CyphailResponse.error("bad statement"));

        assertTrue(output.contains("ERROR: bad statement"));
    }
}
