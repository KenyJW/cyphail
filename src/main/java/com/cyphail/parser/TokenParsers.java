/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.TToken;
import com.cyphail.lexer.TokenString;

public final class TokenParsers {

    private TokenParsers() {
    }

    public static Parser<InputTokens, TokenString, String> Token(TToken tipo) {

        return (InputTokens source) -> {

            if (source.isEmpty()) {
                return new Fail<>("Expected %s but input ended at token %d"
                        .formatted(tipo, source.index()));
            }

            var token = source.first();

            if (token.type() != tipo) {
                return new Fail<>("Expected %s but found %s at token %d"
                        .formatted(tipo, token.type(), source.index()));
            }

            return new Ok<>(token, source.next());
        };
    }

    // Reemplaza la razon del fallo de p por una escrita a mano.
    // Sin esto, un Or fallido reporta la razon de su ULTIMA alternativa,
    // que casi nunca es la que el usuario queria escribir.
    public static <T> Parser<InputTokens, T, String> Label(Parser<InputTokens, T, String> p,
                                                           String expected) {

        return (InputTokens source) -> switch (p.parse(source)) {

            case Fail<InputTokens, T, String> fail -> new Fail<>(
                    "Expected %s but found %s at token %d"
                            .formatted(expected, found(source), source.index()));

            case Ok<InputTokens, T, String> ok -> ok;
        };
    }

    // Que hay en la posicion actual, para el mensaje de error.
    private static String found(InputTokens source) {
        return source.isEmpty() ? "end of input" : source.first().type().toString();
    }
}
