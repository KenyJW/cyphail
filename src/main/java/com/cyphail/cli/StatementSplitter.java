/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// Corta un texto en estatutos separados por ';'.
// Lo usan el REPL (para saber si sigue pidiendo lineas) y .load (para el archivo).
public final class StatementSplitter {

    // Los estatutos completos, y lo que quedo despues del ultimo ';'.
    public record Split(List<String> statements, String rest) {
    }

    // Fuera de cualquier cadena.
    private static final char NONE = '\0';

    // Lo que se sabe en un punto del recorrido: los estatutos ya cortados,
    // donde empieza el actual, y que comilla abrio la cadena en curso.
    private record State(List<String> done, int start, char quote) {
    }

    private StatementSplitter() {
    }

    public static Split split(String text) {
        var end = IntStream.range(0, text.length())
                .boxed()
                .reduce(new State(List.of(), 0, NONE),
                        (state, index) -> step(text, state, index),
                        (a, b) -> {
                            // el stream es secuencial; este combinador nunca corre
                            throw new UnsupportedOperationException("sequential only");
                        });

        return new Split(end.done(), text.substring(end.start()).strip());
    }

    // Guarda el indice en vez de ir concatenando: cortar es un substring al final,
    // asi el recorrido es lineal aunque el texto sea un archivo entero.
    private static State step(String text, State state, int index) {
        var current = text.charAt(index);

        // Dentro de una cadena solo la cierra la misma comilla que la abrio.
        // Por eso "O'Brien" no abre una cadena nueva en la comilla simple.
        if (state.quote() != NONE) {
            return current == state.quote()
                    ? new State(state.done(), state.start(), NONE)
                    : state;
        }

        if (current == '\'' || current == '"') {
            return new State(state.done(), state.start(), current);
        }

        if (current == ';') {
            var statement = text.substring(state.start(), index).strip();
            var done = statement.isEmpty() ? state.done() : plus(state.done(), statement);
            return new State(done, index + 1, NONE);
        }

        return state;
    }

    // Retorna una lista nueva con el elemento al final: nada se modifica en el sitio.
    private static List<String> plus(List<String> list, String item) {
        return Stream.concat(list.stream(), Stream.of(item)).toList();
    }
}
