/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;
import java.util.List;

// REMOVE p.discount
public record RemoveStatement(List<PropertyAccessExpression> targets) implements Statement {}
