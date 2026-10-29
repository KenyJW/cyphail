/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

// Las variables del estatuto y de que tipo es cada una, en orden de
// primera aparicion. El codegen la amplia con sus propios nombres Prolog.
public record VariableTable(Map<String, VariableKind> variables) {

    public VariableTable {
        // Map.copyOf NO conserva el orden de insercion: su iteracion cambia
        // entre ejecuciones de la JVM. LinkedHashMap si lo conserva.
        variables = Collections.unmodifiableMap(new LinkedHashMap<>(variables));
    }

    public static VariableTable of(LinkedHashMap<String, VariableKind> variables) {
        return new VariableTable(variables);
    }
}
