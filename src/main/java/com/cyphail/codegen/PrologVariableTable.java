/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Prolog Variable Mapping)
 */
package com.cyphail.codegen;

import com.cyphail.ast.VariableKind;
import com.cyphail.ast.VariableTable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

public final class PrologVariableTable {

    public static final String ROWS = "Rows";
    public static final String QUERY_ID = "__QueryId";

    private final Map<String, Binding> bindings;

    private PrologVariableTable(
            Map<String, Binding> bindings
    ) {
        this.bindings = Collections.unmodifiableMap(
                new LinkedHashMap<>(bindings)
        );
    }

    public static PrologVariableTable from(
            VariableTable variableTable
    ) {
        Objects.requireNonNull(
                variableTable,
                "variableTable cannot be null"
        );

        LinkedHashMap<String, Binding> result =
                new LinkedHashMap<>();

        Set<String> usedNames = new HashSet<>();

        variableTable.variables().forEach(
                (sourceName, kind) -> {
                    String baseName =
                            "__" + sourceName.toUpperCase(
                                    Locale.ROOT
                            );

                    String generatedName =
                            uniqueName(baseName, usedNames);

                    result.put(
                            sourceName,
                            new Binding(
                                    kind,
                                    generatedName
                            )
                    );
                }
        );

        return new PrologVariableTable(result);
    }

    public Map<String, Binding> bindings() {
        return bindings;
    }

    public Binding binding(String sourceName) {
        Objects.requireNonNull(
                sourceName,
                "sourceName cannot be null"
        );

        Binding binding = bindings.get(sourceName);

        if (binding == null) {
            throw new IllegalArgumentException(
                    "Undefined Cyphail variable: "
                            + sourceName
            );
        }

        return binding;
    }

    public String variable(String sourceName) {
        return binding(sourceName).prologName();
    }

    public String property(
            String sourceName,
            String propertyName
    ) {
        Objects.requireNonNull(
                propertyName,
                "propertyName cannot be null"
        );

        if (propertyName.isBlank()) {
            throw new IllegalArgumentException(
                    "propertyName cannot be blank"
            );
        }

        return variable(sourceName) + propertyName;
    }

    private static String uniqueName(
            String baseName,
            Set<String> usedNames
    ) {
        String candidate = baseName;
        int suffix = 2;

        while (!usedNames.add(candidate)) {
            candidate = baseName + "_" + suffix;
            suffix++;
        }

        return candidate;
    }

    public record Binding(
            VariableKind kind,
            String prologName
    ) {
        public Binding {
            Objects.requireNonNull(
                    kind,
                    "kind cannot be null"
            );
            Objects.requireNonNull(
                    prologName,
                    "prologName cannot be null"
            );
        }
    }
}