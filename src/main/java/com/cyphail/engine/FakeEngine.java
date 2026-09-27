/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Engine Broker / Fake Engine)
 */
package com.cyphail.engine;

import com.cyphail.engine.data.FakeResponseData;
import com.cyphail.engine.data.JsonFakeDataSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public final class FakeEngine implements EngineBroker {

    private final JsonFakeDataSource dataSource;

    public FakeEngine() {
        this.dataSource = new JsonFakeDataSource(
                Path.of("data", "fake-responses.json")
        );
    }

    @Override
    public EngineResult execute(String statement) {
        if (statement == null || statement.isBlank()) {
            return EngineResult.error("Empty statement.");
        }

        String trimmed = statement.strip();

        try {
            var response = dataSource.findByQuery(trimmed);

            if (response.isPresent()) {
                return createResult(response.get());
            }
        } catch (IOException exception) {
            return EngineResult.error(
                    "Could not read fake data: " + exception.getMessage()
            );
        }

        String upper = trimmed.toUpperCase(Locale.ROOT);

        if (upper.startsWith("MATCH") && upper.contains("RETURN")) {
            return EngineResult.ok(genericQueryResult(trimmed));
        }

        if (upper.startsWith("CREATE")
                || upper.startsWith("SET")
                || upper.startsWith("DELETE")
                || upper.startsWith("DETACH")) {
            return EngineResult.ok(
                    "OK. Statement executed after "
                            + fakeElapsedMs(1, 30)
                            + " ms."
            );
        }

        return EngineResult.ok(
                "OK. Statement received after "
                        + fakeElapsedMs(1, 10)
                        + " ms."
        );
    }

    private static EngineResult createResult(FakeResponseData response) {
        String output;

        if (response.columns().isEmpty()) {
            output = response.message();
        } else {
            output = table(response.columns(), response.rows());

            if (!response.message().isBlank()) {
                output += "\n" + response.message();
            }
        }

        if (response.success()) {
            return EngineResult.ok(output);
        }

        return EngineResult.error(output);
    }

    private static String genericQueryResult(String statement) {
        String body = table(
                List.of("result"),
                List.of(
                        List.of("\"(fake result for: " + statement + ")\"")
                )
        );

        return body
                + "\nOK. Query resolved after "
                + fakeElapsedMs(5, 100)
                + " ms.";
    }

    private static String table(
            List<String> headers,
            List<List<String>> rows
    ) {
        int columns = headers.size();
        int[] widths = new int[columns];

        for (int i = 0; i < columns; i++) {
            widths[i] = headers.get(i).length();
        }

        for (List<String> row : rows) {
            for (int i = 0; i < columns; i++) {
                widths[i] = Math.max(
                        widths[i],
                        row.get(i).length()
                );
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append(formatRow(headers, widths));

        int separatorWidth = 0;

        for (int width : widths) {
            separatorWidth += width + 4;
        }

        sb.append("-".repeat(
                Math.max(separatorWidth - 2, 10)
        )).append('\n');

        for (List<String> row : rows) {
            sb.append(formatRow(row, widths));
        }

        return sb.substring(0, sb.length() - 1);
    }

    private static String formatRow(
            List<String> cells,
            int[] widths
    ) {
        StringBuilder row = new StringBuilder();

        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i);

            row.append(cell).append(
                    " ".repeat(
                            Math.max(
                                    1,
                                    widths[i] + 4 - cell.length()
                            )
                    )
            );
        }

        return row.toString().stripTrailing() + "\n";
    }

    private static long fakeElapsedMs(
            long minInclusive,
            long maxInclusive
    ) {
        return ThreadLocalRandom.current()
                .nextLong(minInclusive, maxInclusive + 1);
    }
}