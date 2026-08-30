/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Engine Broker / Fake Engine)
 */
package com.cyphail.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeEngineTest {

    private final EngineBroker engine = new FakeEngine();

    @Test
    void blankStatementFails() {
        EngineResult result = engine.execute("   ");
        assertFalse(result.success());
        assertEquals("Empty statement.", result.output());
    }

    @Test
    void nullStatementFails() {
        EngineResult result = engine.execute(null);
        assertFalse(result.success());
    }

    @Test
    void cannedPersonQueryReturnsExpectedTable() {
        EngineResult result = engine.execute("MATCH (p:Persona) RETURN p.nombre, p.edad");
        assertTrue(result.success());
        assertTrue(result.output().contains("p.nombre"));
        assertTrue(result.output().contains("\"Ana\""));
        assertTrue(result.output().contains("OK. Query available after 42 ms."));
    }

    @Test
    void cannedFriendsQueryReturnsExpectedTable() {
        String query = "MATCH (p1:Persona)-[r:AMIGO_DE]->(p2:Persona) "
                + "RETURN p1.nombre AS Persona, type(r) AS Relacion, p2.nombre AS AmigoDe";
        EngineResult result = engine.execute(query);
        assertTrue(result.success());
        assertTrue(result.output().contains("AMIGO_DE"));
        assertTrue(result.output().contains("OK. Query resolved after 666 ms."));
    }

    @Test
    void unknownMatchReturnQueryStillProducesATable() {
        EngineResult result = engine.execute("MATCH (x:Whatever) RETURN x");
        assertTrue(result.success());
        assertTrue(result.output().contains("OK. Query resolved after"));
    }

    @Test
    void createStatementReturnsExecutionConfirmation() {
        EngineResult result = engine.execute("CREATE (a:Person {name: \"Ana\"})");
        assertTrue(result.success());
        assertTrue(result.output().startsWith("OK. Statement executed after"));
    }
}
