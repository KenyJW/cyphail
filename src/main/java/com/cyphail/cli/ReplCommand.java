/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 Modificado por: Jose Moya Perez (Integracion SWI-Prolog)
 */
package com.cyphail.cli;

import com.cyphail.frontend.CyphailResponse;
import com.cyphail.frontend.FrontendFactory;
import com.cyphail.frontend.FrontendSession;
import com.cyphail.frontend.RequestHandler;
import picocli.CommandLine.Command;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Callable;

@Command(
        name = "repl",
        description = "Starts Cyphail REPL."
)
public final class ReplCommand implements Callable<Integer> {

    // El grafo con el que abre la sesion. La guia pide que los casos corran
    // contra un grafo ya disponible al levantar el servidor, sin un .load
    // manual de por medio.
    private static final String BASE_GRAPH = "casos-p2/grafo.cyphail";

    private final RequestHandler injectedHandler;
    private final InputStream in;
    private final PrintStream out;
    private final Optional<Path> baseGraph;

    public ReplCommand() {
        this(null, System.in, System.out, Optional.of(Path.of(BASE_GRAPH)));
    }

    // Sin grafo base: lo usan las pruebas del ciclo, que cuentan exactamente
    // que estatutos recibe el handler y no quieren veinte de regalo.
    ReplCommand(
            RequestHandler handler,
            InputStream in,
            PrintStream out
    ) {
        this(handler, in, out, Optional.empty());
    }

    ReplCommand(
            RequestHandler handler,
            InputStream in,
            PrintStream out,
            Optional<Path> baseGraph
    ) {
        this.injectedHandler = handler;
        this.in = in;
        this.out = out;
        this.baseGraph = baseGraph;
    }

    @Override
    public Integer call() throws IOException {
        if (injectedHandler != null) {
            return runSession(injectedHandler);
        }

        try (
                FrontendSession session =
                        FrontendFactory.createP2Session()
        ) {
            return runSession(session.handler());
        } catch (IOException exception) {
            out.println(
                    "ERROR: SWI-Prolog engine error: "
                            + exception.getMessage()
            );
            return 1;
        }
    }

    private Integer runSession(
            RequestHandler activeHandler
    ) throws IOException {
        out.println(Banner.welcome());
        baseGraph.ifPresent(path -> loadBaseGraph(path, activeHandler));

        try (
                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(in)
                        )
        ) {
            loop(reader, "", activeHandler);
        }

        out.println("Bye!");
        return 0;
    }

    // Carga el grafo con el que abre la sesion. Que falte no es un fallo: el
    // jar se puede correr desde otra carpeta, y ahi se arranca con el grafo
    // vacio y se avisa.
    private void loadBaseGraph(
            Path path,
            RequestHandler activeHandler
    ) {
        if (!Files.exists(path)) {
            out.println(
                    "No base graph at \"%s\": starting with an empty graph."
                            .formatted(path)
            );
            return;
        }

        out.println(summary(LoadCommand.run(path.toString(), activeHandler)));
    }

    // .load imprime una linea por estatuto, y el grafo trae veinte: serian
    // veinte lineas antes del primer prompt. Se resume a una, y si algo fallo
    // se muestran solo los errores, que es lo unico que hay que leer.
    static String summary(String output) {
        var lines = output.lines().toList();
        var errors = lines.stream().filter(line -> line.startsWith("ERROR")).toList();

        return errors.isEmpty()
                ? "Base graph loaded: %d statements.".formatted(lines.size())
                : String.join(System.lineSeparator(), errors);
    }

    private void loop(
            BufferedReader reader,
            String pending,
            RequestHandler activeHandler
    ) throws IOException {
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

        feed.command().ifPresent(
                command -> out.println(
                        dispatchReplCommand(
                                command,
                                activeHandler
                        )
                )
        );

        feed.statements().forEach(
                statement -> out.println(
                        run(statement, activeHandler)
                )
        );

        loop(
                reader,
                feed.pending(),
                activeHandler
        );
    }

    private void warnIfIncomplete(String pending) {
        if (!pending.isBlank()) {
            out.println(
                    "ERROR: incomplete statement discarded "
                            + "(missing \";\")"
            );
        }
    }

    private String dispatchReplCommand(
            String input,
            RequestHandler activeHandler
    ) {
        var parts = input.strip().split(" ", 2);
        var command = parts[0].toLowerCase();
        var argument =
                parts.length > 1
                        ? parts[1].strip()
                        : "";

        return switch (command) {
            case ".help" -> ReplHelp.text();
            case ".about" -> AboutInfo.text();
            case ".use" -> GraphCatalog.use(argument);
            case ".tree" ->
                    com.cyphail.tree.TreeCommand.run(
                            argument
                    );
            case ".load" ->
                    LoadCommand.run(
                            argument,
                            activeHandler
                    );
            default ->
                    "ERROR: unknown REPL command \""
                            + command
                            + "\". Type \".help\" "
                            + "for a list of commands.";
        };
    }

    private void prompt(String pending) {
        out.print(ReplInput.prompt(pending));
        out.flush();
    }

    private String run(
            String statement,
            RequestHandler activeHandler
    ) {
        try {
            return format(
                    activeHandler.handle(statement)
            );
        } catch (RuntimeException problem) {
            return "ERROR: " + message(problem);
        }
    }

    static String message(RuntimeException problem) {
        return problem.getMessage() == null
                ? problem.getClass().getSimpleName()
                : problem.getMessage();
    }

    static String format(CyphailResponse response) {
        return response.state()
                ? response.message()
                : "ERROR: " + response.message();
    }
}