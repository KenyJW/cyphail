/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 Basado en el modelo de combinadores visto en clase (Work.java, 15/09/2026).
 */
package com.cyphail.lexer;

// Tipos de token del dialecto Cyphail (casos de uso 1-15 del SPEC).
public enum TToken {
    // literales e identificadores
    NUM, ID, STRING,

    // palabras clave
    MATCH, WHERE, RETURN, AS, CREATE, SET, DELETE, DETACH,
    NOT, AND, OR, COUNT, SAVE, LOAD, TRUE, FALSE,

    // delimitadores
    LPAREN, RPAREN, LBRACKET, RBRACKET, LBRACE, RBRACE,
    COLON, COMMA, DOT, DOTDOT, SEMICOLON,

    // flechas y operadores
    ARROW_RIGHT,   // ->
    ARROW_LEFT,    // <-
    DASH,          // -
    STAR,          // *
    EQ,            // =
    NEQ,           // <>
    LT, LE,        // <  <=
    GT, GE         // >  >=
}
