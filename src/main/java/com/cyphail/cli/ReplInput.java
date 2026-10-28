/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import java.util.List;
import java.util.Optional;

// Decide que hacer con cada linea que escribe el usuario, sin tocar la consola.
// Al no leer ni imprimir, se prueba sin REPL: ver ReplInputTest.
public final class ReplInput {

    // Lo que el ciclo debe hacer con la linea: ejecutar estos estatutos,
    // guardar este pendiente, y si vino un comando con punto, despacharlo.
    public record Feed(List<String> statements, String pending, Optional<String> command) {

        // Si el ciclo debe terminar.
        public boolean isExit() {
            return command.filter(EXIT::equalsIgnoreCase).isPresent();
        }
    }

    // El prompt depende solo de si falta cerrar algo, asi que vive aqui
    // y no en el ciclo, donde habria que acordarse de actualizarlo.
    public static String prompt(String pending) {
        return pending.isEmpty() ? ">>> " : "... ";
    }

    private static final String EXIT = ".exit";

    private ReplInput() {
    }

    // Retorna que hacer con 'line', dado lo que habia pendiente.
    public static Feed feed(String pending, String line) {
        var trimmed = line.strip();

        // .exit es la unica excepcion: sale aunque haya un estatuto a medias,
        // o el usuario quedaria atrapado sin forma de cerrar la sesion.
        if (trimmed.equalsIgnoreCase(EXIT)) {
            return command(trimmed);
        }

        // Un comando con punto es de una sola linea y no lleva ';'.
        // Solo cuenta si no hay nada pendiente: una linea de continuacion
        // puede empezar con punto, como en "RETURN m" + ".title;".
        if (pending.isEmpty() && trimmed.startsWith(".")) {
            return command(trimmed);
        }

        var split = StatementSplitter.split(join(pending, line));
        return new Feed(split.statements(), split.rest(), Optional.empty());
    }

    private static Feed command(String text) {
        return new Feed(List.of(), "", Optional.of(text));
    }

    // El salto de linea se conserva: el lexer lo trata como espacio y los
    // casos del profesor traen consultas repartidas en varias lineas.
    private static String join(String pending, String line) {
        return pending.isEmpty() ? line : pending + "\n" + line;
    }
}
