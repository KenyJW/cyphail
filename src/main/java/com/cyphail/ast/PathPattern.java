/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.List;

// Un camino: un nodo inicial y cero o mas pasos.
// Un nodo suelto (a) es un camino con la lista de pasos vacia.
public record PathPattern(NodePattern start, List<PatternStep> steps) {}
