/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 */
package com.cyphail.lexer;

// Fallo: la razon por la que el parser no pudo reconocer nada.
public record Fail<I, T, R>(R reason) implements Result<I, T, R> {
}
