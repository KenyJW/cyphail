/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;
public record ComparisonExpression(Expression left, ComparisonOperator operator, Expression right)
        implements Expression {
}