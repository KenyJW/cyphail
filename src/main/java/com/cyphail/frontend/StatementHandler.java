/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Nota: integrante del grupo hasta P1.1; posteriormente salio del curso.
 */
package com.cyphail.frontend;

import com.cyphail.engine.EngineBroker;
import com.cyphail.engine.EngineResult;

import java.util.Objects;

public final class StatementHandler {

    private final EngineBroker engineBroker;

    public StatementHandler(EngineBroker engineBroker) {
        this.engineBroker = Objects.requireNonNull(engineBroker, "engineBroker no puede ser null");
    }

    public CyphailResponse handle(String statement) {
        EngineResult result = engineBroker.execute(statement);
        if (result.success()) {
            return CyphailResponse.ok(result.output());
        }
        return CyphailResponse.error(result.output());
    }
}
