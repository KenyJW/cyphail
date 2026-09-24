/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.Program;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.InputString;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.Result;
import com.cyphail.lexer.TokenString;

import java.util.List;

// Punto de entrada del compilador: texto de Cyphail a Program.
// Encadena lexer y parser para que el resto del sistema no conozca los pasos.
public final class CyphailParser {

    private CyphailParser() {
    }

    // Retorna el Program, o Fail con el mensaje listo para mostrar en el REPL.
    // Un error de sintaxis nunca lanza excepcion: se devuelve como Fail.
    public static Result<InputTokens, Program, String> parse(String source) {

        return switch (Lexers.tokenize(source)) {

            // error del lexer: se reconstruye el Fail porque cambio el tipo
            case Fail<InputString, List<TokenString>, String> fail ->
                    new Fail<>(fail.reason());

            case Ok<InputString, List<TokenString>, String> ok ->
                    StatementParsers.program().parse(new InputTokens(ok.token()));
        };
    }
}
