/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/Compiler)
 */
package com.cyphail.cli;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(name = "compile", description = "Compiles a Cyphail script to Prolog (not implemented in P1.1).")
public final class CompileCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the input .cyphail script.")
    private Path source;

    @Option(names = "--out", description = "Output path for the generated .pl file.")
    private Path outPath;

    private final PrintStream out;

    public CompileCommand() {
        this(System.out);
    }

    CompileCommand(PrintStream out) {
        this.out = out;
    }

    @Override
    public Integer call() {
        out.println("INFO: 'compile' is not implemented yet in P1.1.");
        out.println("      Planned for P1 (Lexer/Parser + Prolog codegen).");
        out.println("      Input: " + source + (outPath != null ? " --out " + outPath : ""));
        return 0;
    }
}
