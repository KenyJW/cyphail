/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog MQI Process Integration Test)
 */
package com.cyphail.engine.mqi;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(
        named = "cyphail.mqi.integration",
        matches = "true"
)
class MqiProcessIntegrationTest {

    @Test
    void startsPrologAndExecutesQuery() throws Exception {
        String executable = System.getProperty(
                "cyphail.swipl",
                "swipl"
        );

        try (
                MqiProcess process = MqiProcess.start(
                        executable,
                        Duration.ofSeconds(10)
                );
                MqiClient client = process.openClient(
                        Duration.ofSeconds(7)
                )
        ) {
            JsonElement response = client.query(
                    "member(X, [automatic_process])"
            );

            assertTrue(
                    response.toString()
                            .contains("automatic_process")
            );
        }
    }
}