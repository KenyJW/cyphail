/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor original: Sebastian Ramirez Calderon
 Modificado por: Jose Moya Perez
 Nota: Sebastian fue integrante del grupo hasta P1.1;
 posteriormente salio del curso.
 */
package com.cyphail.frontend;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(
        named = "cyphail.mqi.integration",
        matches = "true"
)
class FrontendFactoryTest {

    @Test
    void createsTheIntegratedP2Flow()
            throws Exception {

        try (
                FrontendSession session =
                        FrontendFactory.createP2Session()
        ) {
            CyphailResponse response =
                    session.handler().handle(
                            "CREATE "
                                    + "(p:Person "
                                    + "{name: 'Ana'}) "
                                    + "RETURN "
                                    + "p.name AS name"
                    );

            assertTrue(response.state());
            assertTrue(
                    response.message().contains("Ana")
            );
            assertTrue(
                    response.message()
                            .contains(
                                    "OK. Query resolved."
                            )
            );
        }
    }
}