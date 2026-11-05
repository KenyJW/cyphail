/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // Retorna la tabla del programa completo. Solo los patrones definen
    // variables: SET, REMOVE, DELETE y el RETURN unicamente las usan.
    //
    // El merge (first, repeated) -> first es la regla de "primera aparicion":
    // si "p" vuelve a salir, la entrada vieja queda y el orden no se altera.
    public static VariableTable from(Program program) {
        var entries = program.clauses().stream()
                .flatMap(clause -> clause.paths().stream())
                .flatMap(VariableTable::entriesOf);

        return new VariableTable(entries.collect(
                Collectors.toMap(Entry::getKey, Entry::getValue,
                                 (first, repeated) -> first,
                                 LinkedHashMap::new)));
    }

    // Las variables de un camino en el orden en que se escribieron: el nodo
    // inicial y, por cada paso, su relacion y despues su nodo de llegada.
    private static Stream<Entry<String, VariableKind>> entriesOf(PathPattern path) {
        return Stream.concat(
                entry(path.start().variable(), VariableKind.NODE),
                path.steps().stream().flatMap(step -> Stream.concat(
                        entry(step.relationship().variable(), VariableKind.RELATIONSHIP),
                        entry(step.node().variable(), VariableKind.NODE))));
    }

    // Una variable que no se escribio no aporta entrada: el flujo de un
    // Optional vacio tambien es vacio, y la concatenacion lo salta.
    private static Stream<Entry<String, VariableKind>> entry(Optional<String> variable,
                                                             VariableKind kind) {
        return variable.stream().map(name -> Map.entry(name, kind));
    }
}
