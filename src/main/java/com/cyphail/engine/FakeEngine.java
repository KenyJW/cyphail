/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Engine Broker / Fake Engine)
 */
package com.cyphail.engine;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public final class FakeEngine implements EngineBroker {

    private static final Map<String, String> CANNED_QUERIES = Map.of(
            "MATCH (p:Persona) RETURN p.nombre, p.edad",
            table(
                    List.of("p.nombre", "p.edad"),
                    List.of(
                            List.of("\"Ana\"", "28"),
                            List.of("\"Luis\"", "31"),
                            List.of("\"Carlos\"", "25"),
                            List.of("\"Beatriz\"", "34"),
                            List.of("\"David\"", "29"),
                            List.of("\"Elena\"", "22")
                    )
            ) + "\nOK. Query available after 42 ms.",

            "MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) RETURN p1.nombre AS Persona, type(r) AS Relacion, p2.nombre AS AmigoDe",
            table(
                    List.of("Persona", "Relacion", "AmigoDe"),
                    List.of(
                            List.of("\"Ana\"", "\"AMIGO_DE\"", "\"Luis\""),
                            List.of("\"Ana\"", "\"AMIGO_DE\"", "\"Beatriz\""),
                            List.of("\"Luis\"", "\"AMIGO_DE\"", "\"Carlos\""),
                            List.of("\"Luis\"", "\"AMIGO_DE\"", "\"David\""),
                            List.of("\"Carlos\"", "\"AMIGO_DE\"", "\"Elena\""),
                            List.of("\"Beatriz\"", "\"AMIGO_DE\"", "\"Elena\"")
                    )
            ) + "\nOK. Query resolved after 666 ms."
    );

    @Override
    public EngineResult execute(String statement) {
        if (statement == null || statement.isBlank()) {
            return EngineResult.error("Empty statement.");
        }

        String trimmed = statement.strip();
        String canned = CANNED_QUERIES.get(trimmed);
        if (canned != null) {
            return EngineResult.ok(canned);
        }

        String upper = trimmed.toUpperCase(Locale.ROOT);
        if (upper.startsWith("MATCH") && upper.contains("RETURN")) {
            return EngineResult.ok(genericQueryResult(trimmed));
        }
        if (upper.startsWith("CREATE") || upper.startsWith("SET")
                || upper.startsWith("DELETE") || upper.startsWith("DETACH")) {
            return EngineResult.ok("OK. Statement executed after " + fakeElapsedMs(1, 30) + " ms.");
        }
        return EngineResult.ok("OK. Statement received after " + fakeElapsedMs(1, 10) + " ms.");
    }

    private static String genericQueryResult(String statement) {
        String body = table(
                List.of("result"),
                List.of(List.of("\"(fake result for: " + statement + ")\""))
        );
        return body + "\nOK. Query resolved after " + fakeElapsedMs(5, 100) + " ms.";
    }

    private static String table(List<String> headers, List<List<String>> rows) {
        int columns = headers.size();
        int[] widths = new int[columns];
        for (int i = 0; i < columns; i++) {
            widths[i] = headers.get(i).length();
        }
        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(widths[i], row.get(i).length());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatRow(headers, widths));
        int separatorWidth = 0;
        for (int width : widths) {
            separatorWidth += width + 4;
        }
        sb.append("-".repeat(Math.max(separatorWidth - 2, 10))).append('\n');
        for (List<String> row : rows) {
            sb.append(formatRow(row, widths));
        }
        return sb.substring(0, sb.length() - 1);
    }

    private static String formatRow(List<String> cells, int[] widths) {
        StringBuilder row = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);
            row.append(cell).append(" ".repeat(Math.max(1, widths[i] + 4 - cell.length())));
        }
        return row.toString().stripTrailing() + "\n";
    }

    private static long fakeElapsedMs(long minInclusive, long maxInclusive) {
        return ThreadLocalRandom.current().nextLong(minInclusive, maxInclusive + 1);
    }
}
