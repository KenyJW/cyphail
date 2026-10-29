/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

// Conjuncion en el WHERE: a.age > 18 AND a.age = b.age
public record AndExpression(Expression left, Expression right) implements Expression {}
