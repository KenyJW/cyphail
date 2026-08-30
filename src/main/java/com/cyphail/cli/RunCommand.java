/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import com.cyphail.frontend.CyphailResponse;
import com.cyphail.frontend.FrontendFactory;
import com.cyphail.frontend.RequestHandler;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;

@Command(name = "run", description = "Executes a Cyphail script line by line.")
public final class RunCommand implements Callable<Integer> {

    @Parameters(index = "0", description = "Path to the .cyphail script to run.")
    Path script;

    private final RequestHandler handler;
    private final PrintStream out;

    public RunCommand() {
        this(FrontendFactory.createP11Handler());
    }

    RunCommand(RequestHandler handler) {
        this(handler, System.out);
    }

    RunCommand(RequestHandler handler, PrintStream out) {
        this.handler = handler;
        this.out = out;
    }

    @Override
    public Integer call() throws IOException {
        if (!Files.exists(script)) {
            out.println("ERROR: file not found: " + script);
            return 1;
        }
        List<String> lines = Files.readAllLines(script);
        for (String rawLine : lines) {
            String line = rawLine.strip();
            if (line.isEmpty() || line.startsWith("//")) {
                continue;
            }
            printResponse(handler.handle(line));
        }
        return 0;
    }

    private void printResponse(CyphailResponse response) {
        if (response.state()) {
            out.println(response.message());
        } else {
            out.println("ERROR: " + response.message());
        }
    }
}
