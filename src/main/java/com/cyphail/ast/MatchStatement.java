/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.List;
import java.util.Optional;

// Los patrones son caminos (P2): un nodo suelto es un camino de cero pasos.
public record MatchStatement(List<PathPattern> patterns, Optional<Expression> where) implements Statement {}