/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Fake Engine / JSON Data)
 */
package com.cyphail.engine.data;

import java.util.List;

public record FakeResponseCatalog(
        List<FakeResponseData> responses
) {
    public FakeResponseCatalog {
        responses = responses == null
                ? List.of()
                : List.copyOf(responses);
    }
}