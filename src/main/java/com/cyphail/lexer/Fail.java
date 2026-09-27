/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Basado en el modelo de combinadores visto en clase (Work.java, 15/09/2026).
 */
package com.cyphail.lexer;

// Fallo: la razon por la que el parser no pudo reconocer nada.
public record Fail<I, T, R>(R reason) implements Result<I, T, R> {
}
