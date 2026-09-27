/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 */
package com.cyphail.lexer;

// Un parser es una funcion: recibe una entrada y devuelve un Result.
// Por eso es una interfaz funcional: cualquier lambda con esa forma es un parser.
@FunctionalInterface
public interface Parser<I, T, R> {
    Result<I, T, R> parse(I source);
}
