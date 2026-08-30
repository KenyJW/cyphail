/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CyphailCommandTest {

    @Test
    void replSubcommandIsRegistered() {
        CommandLine cmd = new CommandLine(new CyphailCommand());
        CommandLine.ParseResult result = cmd.parseArgs("repl");
        assertInstanceOf(ReplCommand.class, result.subcommand().commandSpec().userObject());
    }

    @Test
    void runSubcommandParsesScriptArgument() {
        CommandLine cmd = new CommandLine(new CyphailCommand());
        CommandLine.ParseResult result = cmd.parseArgs("run", "examples/movies.cyphail");
        Object sub = result.subcommand().commandSpec().userObject();
        assertInstanceOf(RunCommand.class, sub);
        assertEquals(Path.of("examples/movies.cyphail"), ((RunCommand) sub).script);
    }

    @Test
    void helpCommandIsAvailable() {
        CommandLine cmd = new CommandLine(new CyphailCommand());
        int exitCode = cmd.execute("help", "repl");
        assertEquals(0, exitCode);
    }
}
