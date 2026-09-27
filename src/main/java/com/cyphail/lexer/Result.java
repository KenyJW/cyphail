/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Basado en el modelo de combinadores visto en clase (Work.java, 15/09/2026).
 */
package com.cyphail.lexer;

// Resultado de un parser: o comio algo (Ok) o no era el adecuado (Fail).
public sealed interface Result<I, T, R> permits Ok, Fail {
}
