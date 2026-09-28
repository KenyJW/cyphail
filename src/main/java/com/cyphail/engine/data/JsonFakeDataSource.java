/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (JSON Fake Data Source)
 */

package com.cyphail.engine.data;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class JsonFakeDataSource {

    private final Path file;
    private final Gson gson;

    public JsonFakeDataSource(Path file) {
        this.file = Objects.requireNonNull(file, "file cannot be null");
        this.gson = new Gson();
    }

    public Optional<FakeResponseData> findByQuery(String query)
            throws IOException {

        try (Reader reader = Files.newBufferedReader(
                file,
                StandardCharsets.UTF_8
        )) {
            FakeResponseCatalog catalog =
                    gson.fromJson(reader, FakeResponseCatalog.class);

            if (catalog == null) {
                return Optional.empty();
            }

            String normalizedQuery = normalize(query);

            return catalog.responses()
                    .stream()
                    .filter(response ->
                            normalize(response.query())
                                    .equals(normalizedQuery))
                    .findFirst();

        } catch (JsonSyntaxException | JsonIOException exception) {
            throw new IOException(
                    "Invalid fake data file: " + file,
                    exception
            );
        }
    }

    private static String normalize(String query) {
        if (query == null) {
            return "";
        }

        return query.strip().replaceAll("\\s+", " ");
    }
}