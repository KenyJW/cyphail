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
            loop(reader, "");
        }
        out.println("Bye!");
        return 0;
    }

    // Lee una linea, decide con ReplInput y sigue con lo que quedo pendiente.
    // El pendiente viaja como parametro: no hay variable que se reasigne.
    private void loop(BufferedReader reader, String pending) throws IOException {
        prompt(pending);

        var line = reader.readLine();
        if (line == null) {
            warnIfIncomplete(pending);
            return;
        }

        var feed = ReplInput.feed(pending, line);
        if (feed.isExit()) {
            warnIfIncomplete(pending);
            return;
        }

        feed.command().ifPresent(command -> out.println(dispatchReplCommand(command)));
        feed.statements().forEach(statement -> out.println(run(statement)));

        loop(reader, feed.pending());
    }

    // Un estatuto a medias no se ejecuta, pero se avisa: callarlo haria
    // pensar que la consulta si corrio.
    private void warnIfIncomplete(String pending) {
        if (!pending.isBlank()) {
            out.println("ERROR: incomplete statement discarded (missing \";\")");
        }
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
            case ".load" -> LoadCommand.run(argument, handler);
            default -> "ERROR: unknown REPL command \"" + command + "\". Type \".help\" for a list of commands.";
        };
}


    private void prompt(String pending) {
        out.print(ReplInput.prompt(pending));
        out.flush();
    }

    // Un fallo inesperado del motor se muestra como error y la sesion sigue.
    // Sin esto, cualquier excepcion imprime un stack trace y mata el REPL.
    private String run(String statement) {
        try {
            return format(handler.handle(statement));
        } catch (RuntimeException problem) {
            return "ERROR: " + message(problem);
        }
    }

    static String message(RuntimeException problem) {
        return problem.getMessage() == null
                ? problem.getClass().getSimpleName()
                : problem.getMessage();
    }

    private void printResponse(CyphailResponse response) {
        out.println(format(response));
    }

    // Como se ve una respuesta. Lo usa tambien .load, para que un estatuto
    // se lea igual venga de la consola o de un archivo.
    static String format(CyphailResponse response) {
        return response.state() ? response.message() : "ERROR: " + response.message();
    }
}
