/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog Engine Broker Integration Test)
 */
package com.cyphail.engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(
        named = "cyphail.mqi.integration",
        matches = "true"
)
class SwiPrologEngineBrokerIntegrationTest {

    @Test
    void startsPrologAndReturnsRowsBinding()
            throws Exception {
        String executable = System.getProperty(
                "cyphail.swipl",
                "swipl"
        );

        try (
                SwiPrologEngineBroker broker =
                        SwiPrologEngineBroker.start(
                                executable,
                                Duration.ofSeconds(10)
                        )
        ) {
            EngineResult result = broker.execute(
                    "fake query",
                    "Rows = fake_result"
            );

            assertTrue(result.success());
            assertTrue(
                    result.output().contains("fake_result")
            );
        }
    }
}