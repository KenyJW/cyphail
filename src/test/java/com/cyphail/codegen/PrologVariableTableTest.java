/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Prolog Variable Mapping Tests)
 */
package com.cyphail.codegen;

import com.cyphail.ast.VariableKind;
import com.cyphail.ast.VariableTable;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrologVariableTableTest {

    @Test
    void mapsVariablesInFirstAppearanceOrder() {
        LinkedHashMap<String, VariableKind> source =
                new LinkedHashMap<>();

        source.put("a", VariableKind.NODE);
        source.put("r", VariableKind.RELATIONSHIP);
        source.put("b", VariableKind.NODE);

        PrologVariableTable table =
                PrologVariableTable.from(
                        VariableTable.of(source)
                );

        assertEquals(
                List.of("a", "r", "b"),
                table.bindings().keySet().stream()
                        .toList()
        );

        assertEquals("__A", table.variable("a"));
        assertEquals("__R", table.variable("r"));
        assertEquals("__B", table.variable("b"));

        assertEquals(
                VariableKind.NODE,
                table.binding("a").kind()
        );

        assertEquals(
                VariableKind.RELATIONSHIP,
                table.binding("r").kind()
        );
    }

    @Test
    void createsPropertyCalculationNames() {
        LinkedHashMap<String, VariableKind> source =
                new LinkedHashMap<>();

        source.put("a", VariableKind.NODE);

        PrologVariableTable table =
                PrologVariableTable.from(
                        VariableTable.of(source)
                );

        assertEquals(
                "__Aname",
                table.property("a", "name")
        );

        assertEquals(
                "__Aage",
                table.property("a", "age")
        );

        assertEquals(
                "Rows",
                PrologVariableTable.ROWS
        );

        assertEquals(
                "__QueryId",
                PrologVariableTable.QUERY_ID
        );
    }

    @Test
    void rejectsUndefinedVariables() {
        PrologVariableTable table =
                PrologVariableTable.from(
                        VariableTable.of(
                                new LinkedHashMap<>()
                        )
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> table.variable("missing")
        );
    }
}