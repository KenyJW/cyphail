/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog MQI Response Decoder Tests)
 */
package com.cyphail.engine.mqi;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MqiResponseDecoderTest {

    @Test
    void extractsRowsBindingFromMqiResponse() {
        JsonElement response = JsonParser.parseString(
                """
                {
                  "args": [
                    [
                      [
                        {
                          "args": [
                            "Rows",
                            "a.name b.name\\njuan maria"
                          ],
                          "functor": "="
                        }
                      ]
                    ]
                  ],
                  "functor": "true"
                }
                """
        );

        Optional<String> rows =
                MqiResponseDecoder.findTextBinding(
                        response,
                        "Rows"
                );

        assertTrue(rows.isPresent());

        assertEquals(
                "a.name b.name\njuan maria",
                rows.get()
        );
    }

    @Test
    void returnsEmptyWhenBindingDoesNotExist() {
        JsonElement response = JsonParser.parseString(
                """
                {
                  "args": [],
                  "functor": "false"
                }
                """
        );

        Optional<String> rows =
                MqiResponseDecoder.findTextBinding(
                        response,
                        "Rows"
                );

        assertTrue(rows.isEmpty());
    }
}