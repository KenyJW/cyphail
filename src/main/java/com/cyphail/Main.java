/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail;

import com.cyphail.cli.CyphailCommand;
import picocli.CommandLine;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new CyphailCommand()).execute(args);
        System.exit(exitCode);
    }
}
