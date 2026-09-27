/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Engine Broker / Fake Engine)
 */
package com.cyphail.engine;

@FunctionalInterface
public interface EngineBroker {

    EngineResult execute(String statement);

    default EngineResult execute(
            String originalStatement,
            String generatedCode
    ) {
        return execute(originalStatement);
    }
}