/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

// Disyuncion en el WHERE: p.age < 18 OR p.age > 65
public record OrExpression(Expression left, Expression right) implements Expression {}
