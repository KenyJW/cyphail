/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog MQI Integration Test)
 */
package com.cyphail.engine.mqi;

import com.google.gson.JsonElement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@EnabledIfSystemProperty(
        named = "cyphail.mqi.integration",
        matches = "true"
)
class MqiClientIntegrationTest {

    @Test
    void connectsAndReceivesFakeQueryResult() throws Exception {
        try (MqiClient client = new MqiClient(
                "127.0.0.1",
                4242,
                "cyphail",
                Duration.ofSeconds(7)
        )) {
            JsonElement response = client.query(
                    "member(X, [fake_result])"
            );

            assertTrue(response.isJsonObject());

            assertEquals(
                    "true",
                    response.getAsJsonObject()
                            .get("functor")
                            .getAsString()
            );

            assertTrue(
                    response.toString()
                            .contains("fake_result")
            );
        }
    }
}