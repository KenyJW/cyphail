/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Nota: integrante del grupo hasta P1.1; posteriormente salio del curso.
 */
package com.cyphail.frontend;

import com.cyphail.engine.EngineBroker;
import com.cyphail.engine.EngineResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRouterTest {

    @Test
    void rejectsBlankInputWithoutCallingEngine() {
        AtomicReference<String> received = new AtomicReference<>();
        EngineBroker engine = statement -> {
            received.set(statement);
            return EngineResult.ok("should not run");
        };

        CyphailResponse response = new FrontendRouter(engine).handle("  ");

        assertFalse(response.state());
        assertEquals("Empty command.", response.message());
        assertNull(received.get());
    }

    @Test
    void trimsAndForwardsStatementToEngine() {
        AtomicReference<String> received = new AtomicReference<>();
        EngineBroker engine = statement -> {
            received.set(statement);
            return EngineResult.ok("executed");
        };

        CyphailResponse response = new FrontendRouter(engine)
                .handle("  MATCH (p:Person) RETURN p.name  ");

        assertTrue(response.state());
        assertEquals("executed", response.message());
        assertEquals("MATCH (p:Person) RETURN p.name", received.get());
    }

    @Test
    void adaptsEngineFailureToCliError() {
        EngineBroker engine = statement -> EngineResult.error("Engine error.");

        CyphailResponse response = new FrontendRouter(engine).handle("MATCH (p)");

        assertFalse(response.state());
        assertEquals("Engine error.", response.message());
    }
}
