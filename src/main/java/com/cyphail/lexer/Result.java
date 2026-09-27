/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 */
package com.cyphail.lexer;

// Resultado de un parser: o comio algo (Ok) o no era el adecuado (Fail).
public sealed interface Result<I, T, R> permits Ok, Fail {
}
