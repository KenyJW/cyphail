/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

// Un numero con decimales: 19.99. Aparte de NumberLiteral, que guarda
// enteros en un long: asi los enteros que ya funcionan no cambian.
public record DecimalLiteral(double value) implements LiteralExpression {}
