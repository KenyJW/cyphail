/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.NodePattern;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.TokenString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// Patrones del ejercicio 2 de la sesion 17 (Ejercicios_17-15-09.md).
class EjercicioSesion17Test {

    private static NodePattern parse(String source) {
        var tokens = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var list = (List<TokenString>) tokens.token();

        var ok = assertInstanceOf(Ok.class,
                PatternParsers.nodePattern().parse(new InputTokens(list)));
        return (NodePattern) ok.token();
    }

    @Test
    void variasEtiquetasConEspacios() {
        assertEquals(new NodePattern(Optional.of("p"),
                        List.of("Person", "Employee", "Player"), List.of()),
                parse("( p : Person: Employee:Player)"));
    }

    @Test
    void soloVariable() {
        assertEquals(new NodePattern(Optional.of("p"), List.of(), List.of()), parse("(p)"));
    }

    @Test
    void guionBajoComoVariable() {
        assertEquals(new NodePattern(Optional.of("_"), List.of("Any", "One"), List.of()),
                parse("(_:Any:One)"));
    }

    @Test
    void patronVacio() {
        assertEquals(new NodePattern(Optional.empty(), List.of(), List.of()), parse("()"));
    }
}
