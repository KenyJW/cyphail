package com.cyphail.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import picocli.CommandLine;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompileCommandTest {

    @Test
    void compilesCyphailFileToProlog(
            @TempDir Path tempDirectory
    ) throws Exception {

        Path source =
                tempDirectory.resolve("movies.cyphail");

        Path output =
                tempDirectory.resolve("movies.pl");

        Files.writeString(
                source,
                "MATCH (m:Movie) RETURN m.title AS title"
        );

        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();

        CompileCommand command = new CompileCommand(
                new PrintStream(
                        buffer,
                        true,
                        StandardCharsets.UTF_8
                )
        );

        int exitCode = new CommandLine(command).execute(
                source.toString(),
                "--out",
                output.toString()
        );

        assertEquals(0, exitCode);
        assertTrue(Files.exists(output));

        String generatedCode = Files.readString(output);

        assertTrue(
                generatedCode.startsWith("query([match(")
        );

        assertTrue(
                buffer.toString(StandardCharsets.UTF_8)
                        .contains("OK. Prolog code written to")
        );
    }

    @Test
    void doesNotGenerateFileForUndefinedVariable(
            @TempDir Path tempDirectory
    ) throws Exception {

        Path source =
                tempDirectory.resolve("invalid.cyphail");

        Path output =
                tempDirectory.resolve("invalid.pl");

        Files.writeString(
                source,
                "MATCH (p:Person) "
                        + "WHERE q.age > 60 "
                        + "RETURN q AS name"
        );

        ByteArrayOutputStream buffer =
                new ByteArrayOutputStream();

        CompileCommand command = new CompileCommand(
                new PrintStream(
                        buffer,
                        true,
                        StandardCharsets.UTF_8
                )
        );

        int exitCode = new CommandLine(command).execute(
                source.toString(),
                "--out",
                output.toString()
        );

        assertEquals(1, exitCode);
        assertFalse(Files.exists(output));

        assertTrue(
                buffer.toString(StandardCharsets.UTF_8)
                        .contains("Undefined variable 'q'")
        );
    }
}