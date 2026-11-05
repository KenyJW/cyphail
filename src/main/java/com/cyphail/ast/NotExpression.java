/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

// Negacion en el WHERE: NOT p.active
public record NotExpression(Expression operand) implements Expression {}
