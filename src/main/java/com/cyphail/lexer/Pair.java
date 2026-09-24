/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.lexer;

// Dos resultados juntos, cada uno con su propio tipo.
// Es lo que devuelve And cuando sus dos parsers tienen exito.
public record Pair<A, B>(A first, B second) {
}
