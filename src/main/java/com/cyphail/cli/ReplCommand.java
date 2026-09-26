/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import com.cyphail.frontend.CyphailResponse;
import com.cyphail.frontend.FrontendFactory;
import com.cyphail.frontend.RequestHandler;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.concurrent.Callable;

@Command(name = "repl", description = "Starts Cyphail REPL.")
public final class ReplCommand implements Callable<Integer> {

    private static final String PROMPT = ">>> ";

    private final RequestHandler handler;
    private final InputStream in;
    private final PrintStream out;

    public ReplCommand() {
        this(FrontendFactory.createP11Handler(), System.in, System.out);
    }

    ReplCommand(RequestHandler handler, InputStream in, PrintStream out) {
        this.handler = handler;
        this.in = in;
        this.out = out;
    }

    @Override
    public Integer call() throws IOException {
        out.println(Banner.welcome());
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
            String line;
            prompt();
            while ((line = reader.readLine()) != null) {
                String trimmed = line.strip();

                if (trimmed.isEmpty()) {
                    prompt();
                    continue;
                }
                if (trimmed.equalsIgnoreCase(".exit")) {
                    break;
                }
                if (trimmed.startsWith(".")) {
                    out.println(dispatchReplCommand(trimmed));
                } else {
                    printResponse(handler.handle(trimmed));
                }
                prompt();
            }
        }
        out.println("Bye!");
        return 0;
    }

    private String dispatchReplCommand(String input) {
    String entrada = input.strip();
    String command;
    String argument;


    if (entrada.contains(" ")) {
        String[] partes = entrada.split(" ", 2);
        command = partes[0].toLowerCase(); 
        argument = partes[1].strip();     
    } else {
        command = entrada.toLowerCase();
        argument = ""; 
    }
    return switch (command) {
            case ".help" -> ReplHelp.text();
            case ".about" -> AboutInfo.text();
            case ".use" -> GraphCatalog.use(argument);
            case ".tree" -> com.cyphail.tree.TreeCommand.run(argument);
            default -> "ERROR: unknown REPL command \"" + command + "\". Type \".help\" for a list of commands.";
        };
}


    private void prompt() {
        out.print(PROMPT);
        out.flush();
    }

    private void printResponse(CyphailResponse response) {
        if (response.state()) {
            out.println(response.message());
        } else {
            out.println("ERROR: " + response.message());
        }
    }
}
