/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog MQI Response Decoder)
 */
package com.cyphail.engine.mqi;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MqiResponseDecoder {

    private MqiResponseDecoder() {
    }

    public static Optional<JsonElement> findBinding(
            JsonElement response,
            String variable
    ) {
        Objects.requireNonNull(
                response,
                "response cannot be null"
        );
        Objects.requireNonNull(
                variable,
                "variable cannot be null"
        );

        if (response.isJsonObject()) {
            JsonObject object = response.getAsJsonObject();

            Optional<JsonElement> binding =
                    bindingFromObject(object, variable);

            if (binding.isPresent()) {
                return binding;
            }

            for (Map.Entry<String, JsonElement> entry
                    : object.entrySet()) {
                Optional<JsonElement> nested =
                        findBinding(entry.getValue(), variable);

                if (nested.isPresent()) {
                    return nested;
                }
            }
        }

        if (response.isJsonArray()) {
            for (JsonElement element
                    : response.getAsJsonArray()) {
                Optional<JsonElement> nested =
                        findBinding(element, variable);

                if (nested.isPresent()) {
                    return nested;
                }
            }
        }

        return Optional.empty();
    }

    public static Optional<String> findTextBinding(
            JsonElement response,
            String variable
    ) {
        return findBinding(response, variable)
                .filter(JsonElement::isJsonPrimitive)
                .filter(element ->
                        element.getAsJsonPrimitive().isString()
                )
                .map(JsonElement::getAsString);
    }

    private static Optional<JsonElement> bindingFromObject(
            JsonObject object,
            String variable
    ) {
        if (!object.has("functor")
                || !object.has("args")) {
            return Optional.empty();
        }

        if (!object.get("functor").isJsonPrimitive()
                || !object.get("functor")
                .getAsString()
                .equals("=")) {
            return Optional.empty();
        }

        if (!object.get("args").isJsonArray()) {
            return Optional.empty();
        }

        JsonArray arguments =
                object.getAsJsonArray("args");

        if (arguments.size() != 2) {
            return Optional.empty();
        }

        JsonElement name = arguments.get(0);

        if (!name.isJsonPrimitive()
                || !name.getAsString().equals(variable)) {
            return Optional.empty();
        }

        return Optional.of(arguments.get(1).deepCopy());
    }
}