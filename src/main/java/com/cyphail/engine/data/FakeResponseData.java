/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Fake Engine / JSON Data)
 */
package com.cyphail.engine.data;

import java.util.List;
import java.util.Objects;

public record FakeResponseData(
        String id,
        String query,
        boolean success,
        List<String> columns,
        List<List<String>> rows,
        String message
) {
    public FakeResponseData {
        Objects.requireNonNull(id, "id cannot be null");
        Objects.requireNonNull(query, "query cannot be null");

        columns = columns == null
                ? List.of()
                : List.copyOf(columns);

        rows = rows == null
                ? List.of()
                : rows.stream()
                .map(List::copyOf)
                .toList();

        message = message == null ? "" : message;
    }
}