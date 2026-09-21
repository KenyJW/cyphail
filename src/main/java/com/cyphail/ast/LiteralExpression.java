/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

public sealed interface LiteralExpression extends Expression
permits NumberLiteral, StringLiteral, BooleanLiteral{}