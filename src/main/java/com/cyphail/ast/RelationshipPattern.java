/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.List;
import java.util.Optional;

// Una relacion entre dos nodos: -[r:TIPO {clave: valor}]->
// Solo la direccion es obligatoria; lo demas puede faltar.
public record RelationshipPattern(Optional<String> variable,
                                  Optional<String> type,
                                  List<PropertyEntry> properties,
                                  Direction direction) {}
