package com.cyphail.engine.data;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonFakeDataSourceTest {

    @Test
    void readsFileAgainAfterJsonChanges(
            @TempDir Path tempDirectory
    ) throws IOException {

        Path jsonFile =
                tempDirectory.resolve("fake-responses.json");

        Files.writeString(jsonFile, jsonWithName("Ana"));

        JsonFakeDataSource dataSource =
                new JsonFakeDataSource(jsonFile);

        var firstResponse = dataSource.findByQuery(
                "MATCH (p:Person) RETURN p.name"
        );

        assertTrue(firstResponse.isPresent());
        assertEquals(
                "Ana",
                firstResponse.get().rows().getFirst().getFirst()
        );

        Files.writeString(jsonFile, jsonWithName("Gustavo"));

        var secondResponse = dataSource.findByQuery(
                "MATCH (p:Person) RETURN p.name"
        );

        assertTrue(secondResponse.isPresent());
        assertEquals(
                "Gustavo",
                secondResponse.get().rows().getFirst().getFirst()
        );
    }

    private static String jsonWithName(String name) {
        return """
                {
                  "responses": [
                    {
                      "id": "person-query",
                      "query": "MATCH (p:Person) RETURN p.name",
                      "success": true,
                      "columns": ["p.name"],
                      "rows": [["%s"]],
                      "message": "OK."
                    }
                  ]
                }
                """.formatted(name);
    }
}