/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Basado en el modelo de combinadores visto en clase (Work.java, 15/09/2026).
 */
package com.cyphail.lexer;

// El texto completo y la posicion desde donde se sigue leyendo.
// Es inmutable: avanzar significa crear un InputString nuevo.
public record InputString(String input, int index) {
}
