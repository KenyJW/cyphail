/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;
import java.util.List;

// SET p.precio = 19.99, p.stock = 3
public record SetStatement(List<SetItem> items) implements Statement {}
