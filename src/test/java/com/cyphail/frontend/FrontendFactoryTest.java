/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Nota: integrante del grupo hasta P1.1; posteriormente salio del curso.
 */
package com.cyphail.frontend;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendFactoryTest {

    @Test
    void createsTheIntegratedP11Flow() {
        CyphailResponse response = FrontendFactory.createP11Handler()
                .handle("MATCH (p:Person) RETURN p.name");

        assertTrue(response.state());
        assertTrue(response.message().contains("OK. Query resolved after"));
    }
}
