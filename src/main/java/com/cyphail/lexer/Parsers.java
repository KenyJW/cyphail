/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Combinadores genericos, segun el modelo visto en clase (Work.java, 22/09/2026).
 */
package com.cyphail.lexer;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

// Combinadores: reciben parsers y devuelven parsers.
// Son genericos a proposito: los mismos sirven para el lexer (sobre texto)
// y mas adelante para el parser (sobre una lista de tokens).
public final class Parsers {

    private Parsers() {
    }

    // Prueba p; si falla, prueba q. El primero que reconozca algo gana.
    public static <I, T, R> Parser<I, T, R> Or(Parser<I, T, R> p, Parser<I, T, R> q) {

        Parser<I, T, R> parser = (I source) -> {
            var result = p.parse(source);

            if (!(result instanceof Fail<I, T, R>)) {
                return result;
            }
            return q.parse(source);
        };

        return parser;
    }

    // Corre p y le cambia la forma al resultado con f. No consume nada por su
    // cuenta: el rest pasa intacto. Sirve para convertir tokens en nodos del AST.
    public static <I, T, U, R> Parser<I, U, R> Map(Parser<I, T, R> p, Function<T, U> f) {

        return (I source) -> switch (p.parse(source)) {

            // hay que construir un Fail nuevo: su tipo paso de T a U
            case Fail<I, T, R> fail -> new Fail<>(fail.reason());

            case Ok<I, T, R> ok -> new Ok<>(f.apply(ok.token()), ok.rest());
        };
    }

    // Corre p y despues q, pero q empieza donde p dejo la entrada.
    // Si falla cualquiera de los dos, falla todo con la razon de quien fallo.
    public static <I, A, B, R> Parser<I, Pair<A, B>, R> And(Parser<I, A, R> p, Parser<I, B, R> q) {

        return (I source) -> switch (p.parse(source)) {

            case Fail<I, A, R> fail -> new Fail<>(fail.reason());

            // ok.rest() es la clave: q NO recibe la entrada original
            case Ok<I, A, R> primero -> switch (q.parse(primero.rest())) {

                case Fail<I, B, R> fail -> new Fail<>(fail.reason());

                case Ok<I, B, R> segundo -> new Ok<>(
                        new Pair<>(primero.token(), segundo.token()),
                        segundo.rest());
            };
        };
    }

    // Retorna Optional con el resultado de p, o vacio si p falla.
    // Nunca falla. Cuidado: por eso mismo no se puede meter dentro de Star.
    public static <I, T, R> Parser<I, Optional<T>, R> Opt(Parser<I, T, R> p) {

        return (I source) -> switch (p.parse(source)) {

            // p fallo: entrada intacta, sin consumir nada
            case Fail<I, T, R> fail -> new Ok<>(Optional.<T>empty(), source);

            case Ok<I, T, R> ok -> new Ok<>(Optional.of(ok.token()), ok.rest());
        };
    }

    // Retorna la lista de uno o mas p separados por sep, descartando los sep.
    // Se arma con lo que ya existe: And, Star y Map.
    public static <I, T, S, R> Parser<I, List<T>, R> SepBy(Parser<I, T, R> p, Parser<I, S, R> sep) {

        var tail = Star(And(sep, p));      // los que siguen: (sep p)*
        var full = And(p, tail);           // el primero, y despues el resto

        return Map(full, pair -> Stream.concat(
                        Stream.of(pair.first()),
                        pair.second().stream().map(Pair::second))
                .toList());
    }

    // Retorna la lista de uno o mas p. A diferencia de Star, falla si no hay
    // ninguno, y el fallo conserva la razon del primer intento.
    public static <I, T, R> Parser<I, List<T>, R> Some(Parser<I, T, R> p) {
        return Map(And(p, Star(p)), pair -> Stream.concat(
                        Stream.of(pair.first()),
                        pair.second().stream())
                .toList());
    }

    // Aplica p cero o mas veces y junta los resultados en una lista.
    // Nunca falla: "ninguna repeticion" tambien es un resultado valido.
    public static <I, T, R> Parser<I, List<T>, R> Star(Parser<I, T, R> p) {
        return (I source) -> repetir(p, source);
    }

    // El trabajo de verdad. Devuelve Ok y no Result, porque no tiene forma de fallar.
    private static <I, T, R> Ok<I, List<T>, R> repetir(Parser<I, T, R> p, I source) {

        // CASO BASE: p no reconoce nada aqui -> cero resultados, entrada intacta
        if (!(p.parse(source) instanceof Ok<I, T, R> ok)) {
            return new Ok<>(List.of(), source);
        }

        // CASO RECURSIVO: p consumio algo; que Star siga con lo que quedo.
        // El indice siempre avanza, asi que la recursion termina.
        var resto = repetir(p, ok.rest());

        // el resultado de ahora va ADELANTE de los que trajo la llamada siguiente
        var elementos = Stream.concat(Stream.of(ok.token()),
                                      resto.token().stream())
                              .toList();

        return new Ok<>(elementos, resto.rest());
    }
}
