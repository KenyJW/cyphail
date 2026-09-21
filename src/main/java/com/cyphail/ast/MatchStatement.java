/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.List;
import java.util.Optional;

public record MatchStatement(List<NodePattern> patterns, Optional<Expression> where) implements Statement {}