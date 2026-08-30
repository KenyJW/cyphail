/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Nota: integrante del grupo hasta P1.1; posteriormente salio del curso.
 */
package com.cyphail.frontend;

import com.cyphail.engine.EngineBroker;

import java.util.Objects;

public final class FrontendRouter implements RequestHandler {

    private final StatementHandler statementHandler;

    public FrontendRouter(EngineBroker engineBroker) {
        this(new StatementHandler(engineBroker));
    }

    public FrontendRouter(StatementHandler statementHandler) {
        this.statementHandler = Objects.requireNonNull(
                statementHandler, "statementHandler no puede ser null"
        );
    }

    @Override
    public CyphailResponse handle(String input) {
        if (input == null || input.isBlank()) {
            return CyphailResponse.error("Empty command.");
        }
        return statementHandler.handle(input.strip());
    }
}
