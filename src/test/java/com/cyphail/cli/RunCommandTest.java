/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import com.cyphail.frontend.CyphailResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunCommandTest {

    @Test
    void dispatchesNonEmptyNonCommentLinesToHandler(@TempDir Path tempDir) throws Exception {
        Path script = tempDir.resolve("demo.cyphail");
        Files.writeString(script, """
                // comentario
                MATCH (p:Person) RETURN p.name

                CREATE (a:Person {name: "Ana"})
                """);

        List<String> received = new ArrayList<>();
        RunCommand command = new RunCommand(input -> {
            received.add(input);
            return CyphailResponse.ok("ok: " + input);
        }, capture(new ByteArrayOutputStream()));
        command.script = script;

        Integer exitCode = command.call();

        assertEquals(0, exitCode);
        assertEquals(List.of(
                "MATCH (p:Person) RETURN p.name",
                "CREATE (a:Person {name: \"Ana\"})"
        ), received);
    }

    @Test
    void reportsErrorWhenScriptMissing() throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        RunCommand command = new RunCommand(
                input -> CyphailResponse.ok("unused"), capture(buffer));
        command.script = Path.of("no-existe-este-archivo.cyphail");

        Integer exitCode = command.call();

        assertEquals(1, exitCode);
        assertTrue(buffer.toString(StandardCharsets.UTF_8)
                .contains("ERROR: file not found:"));
    }

    private static PrintStream capture(ByteArrayOutputStream buffer) {
        return new PrintStream(buffer, true, StandardCharsets.UTF_8);
    }
}
