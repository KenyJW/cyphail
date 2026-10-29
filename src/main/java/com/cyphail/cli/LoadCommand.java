/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import com.cyphail.frontend.RequestHandler;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// El comando .load del REPL: lee un archivo .cyphail, lo corta en estatutos
// y los ejecuta en orden. Retorna todo lo que hay que mostrar.
public final class LoadCommand {

    private static final String NL = System.lineSeparator();

    private LoadCommand() {
    }

    public static String run(String argument, RequestHandler handler) {
        var path = unquote(argument);

        if (path.isBlank()) {
            return "ERROR: .load needs a file path";
        }

        try {
            return execute(Files.readString(Path.of(path)), handler);
        } catch (IOException | InvalidPathException problem) {
            return "ERROR: cannot read \"%s\"".formatted(path);
        }
    }

    // El SPEC escribe la ruta entre comillas: .load "somepath/mi_script.cyphail"
    private static String unquote(String argument) {
        var text = argument.strip();
        var quoted = text.length() >= 2
                && (text.startsWith("\"") && text.endsWith("\"")
                    || text.startsWith("'") && text.endsWith("'"));

        return quoted ? text.substring(1, text.length() - 1).strip() : text;
    }

    private static String execute(String source, RequestHandler handler) {
        var split = StatementSplitter.split(withoutComments(source));

        if (split.statements().isEmpty() && split.rest().isBlank()) {
            return "ERROR: no statements found";
        }

        var results = split.statements().stream()
                .map(statement -> runStatement(statement, handler));

        // Lo que quedo sin ';' no se ejecuta, pero se avisa.
        var pending = split.rest().isBlank()
                ? Stream.<String>empty()
                : Stream.of("ERROR: incomplete statement at the end of the file (missing \";\")");

        return Stream.concat(results, pending).collect(Collectors.joining(NL));
    }

    // Un estatuto que revienta no corta la corrida: se reporta y sigue el resto.
    private static String runStatement(String statement, RequestHandler handler) {
        try {
            return ReplCommand.format(handler.handle(statement));
        } catch (RuntimeException problem) {
            return "ERROR: " + ReplCommand.message(problem);
        }
    }

    // Descarta las lineas que empiezan con //, como ya lo hace RunCommand.
    // Solo al inicio de linea: asi un "http://..." dentro de una cadena sobrevive.
    private static String withoutComments(String source) {
        return source.lines()
                .filter(line -> !line.strip().startsWith("//"))
                .collect(Collectors.joining(NL));
    }
}
