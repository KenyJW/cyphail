/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

// Una asignacion del SET: p.precio = 19.99
public record SetItem(PropertyAccessExpression target, Expression value) {}
