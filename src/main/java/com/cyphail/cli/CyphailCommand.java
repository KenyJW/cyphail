/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/Main Command)
 */
package com.cyphail.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.HelpCommand;

import java.io.PrintStream;

@Command(
        name = "cyphail",
        mixinStandardHelpOptions = true,
        version = "cyphail 0.1.0 (P1.1)",
        description = "CLI for the Cyphail graph query engine.",
        subcommands = {
                ReplCommand.class,
                RunCommand.class,
                CompileCommand.class,
                HelpCommand.class
        }
)
public final class CyphailCommand implements Runnable {

    private final PrintStream out;

    public CyphailCommand() {
        this(System.out);
    }

    CyphailCommand(PrintStream out) {
        this.out = out;
    }

    @Override
    public void run() {
        new CommandLine(this).usage(out);
    }
}
