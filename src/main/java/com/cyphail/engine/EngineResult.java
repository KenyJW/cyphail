/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Engine Broker / Fake Engine)
 */
package com.cyphail.engine;

public record EngineResult(boolean success, String output) {

    public static EngineResult ok(String output) {
        return new EngineResult(true, output);
    }

    public static EngineResult error(String output) {
        return new EngineResult(false, output);
    }
}
