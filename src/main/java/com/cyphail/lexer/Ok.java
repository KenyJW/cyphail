/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Basado en el modelo de combinadores visto en clase (Work.java, 15/09/2026).
 */
package com.cyphail.lexer;

// Exito: el token reconocido y el resto del input por consumir.
public record Ok<I, T, R>(T token, I rest) implements Result<I, T, R> {
}
