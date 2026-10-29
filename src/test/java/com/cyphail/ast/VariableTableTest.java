/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableTableTest {

    private static VariableTable withThree() {
        var orden = new LinkedHashMap<String, VariableKind>();
        orden.put("p", VariableKind.NODE);
        orden.put("r", VariableKind.RELATIONSHIP);
        orden.put("e", VariableKind.NODE);
        return new VariableTable(orden);
    }

    @Test
    void keepsTheOrderOfFirstAppearance() {
        // con Map.copyOf este orden cambiaba en cada ejecucion de la JVM
        assertEquals(List.of("p", "r", "e"), List.copyOf(withThree().variables().keySet()));
    }

    @Test
    void remembersWhatEachVariableNames() {
        assertEquals(VariableKind.RELATIONSHIP, withThree().variables().get("r"));
        assertEquals(VariableKind.NODE, withThree().variables().get("p"));
    }

    @Test
    void theTableCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
                () -> withThree().variables().put("x", VariableKind.NODE));
    }

    @Test
    void changingTheSourceMapDoesNotChangeTheTable() {
        var fuente = new LinkedHashMap<String, VariableKind>();
        fuente.put("p", VariableKind.NODE);
        var tabla = new VariableTable(fuente);

        fuente.put("q", VariableKind.NODE);

        assertEquals(1, tabla.variables().size());
    }
}
