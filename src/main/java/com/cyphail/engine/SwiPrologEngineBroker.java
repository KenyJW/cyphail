/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog Engine Broker)
 */
package com.cyphail.engine;

import com.cyphail.engine.mqi.MqiClient;
import com.cyphail.engine.mqi.MqiProcess;
import com.cyphail.engine.mqi.MqiResponseDecoder;
import com.google.gson.JsonElement;

import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

public final class SwiPrologEngineBroker
        implements EngineBroker, AutoCloseable {

    private final MqiProcess process;
    private final MqiClient client;
    private boolean closed;

    private SwiPrologEngineBroker(
            MqiProcess process,
            MqiClient client
    ) {
        this.process = Objects.requireNonNull(
                process,
                "process cannot be null"
        );
        this.client = Objects.requireNonNull(
                client,
                "client cannot be null"
        );
    }

    public static SwiPrologEngineBroker start(
            String executable,
            Duration timeout
    ) throws IOException {
        Objects.requireNonNull(
                executable,
                "executable cannot be null"
        );
        Objects.requireNonNull(
                timeout,
                "timeout cannot be null"
        );

        MqiProcess process = MqiProcess.start(
                executable,
                timeout
        );

        try {
            MqiClient client = process.openClient(timeout);

            return new SwiPrologEngineBroker(
                    process,
                    client
            );
        } catch (IOException exception) {
            process.close();
            throw exception;
        }
    }

    @Override
    public EngineResult execute(String statement) {
        return execute(statement, statement);
    }

    @Override
    public synchronized EngineResult execute(
            String originalStatement,
            String generatedCode
    ) {
        if (closed) {
            return EngineResult.error(
                    "SWI-Prolog engine is closed."
            );
        }

        if (generatedCode == null
                || generatedCode.isBlank()) {
            return EngineResult.error(
                    "Generated Prolog code is empty."
            );
        }

        try {
            JsonElement response = client.query(
                    generatedCode.strip()
            );

            Optional<JsonElement> rows =
                    MqiResponseDecoder.findBinding(
                            response,
                            "Rows"
                    );

            if (rows.isEmpty()) {
                return EngineResult.error(
                        "SWI-Prolog response does not contain "
                                + "the Rows variable: "
                                + response
                );
            }

            return EngineResult.ok(
                    renderBinding(rows.get())
            );
        } catch (IOException exception) {
            return EngineResult.error(
                    "SWI-Prolog communication error: "
                            + exception.getMessage()
            );
        } catch (RuntimeException exception) {
            return EngineResult.error(
                    "Could not decode the SWI-Prolog response: "
                            + exception.getMessage()
            );
        }
    }

    private static String renderBinding(
            JsonElement binding
    ) {
        if (binding.isJsonPrimitive()
                && binding.getAsJsonPrimitive().isString()) {
            return binding.getAsString();
        }

        return binding.toString();
    }

    @Override
    public synchronized void close() throws IOException {
        if (closed) {
            return;
        }

        closed = true;

        IOException failure = null;

        try {
            client.close();
        } catch (IOException exception) {
            failure = exception;
        } finally {
            process.close();
        }

        if (failure != null) {
            throw failure;
        }
    }
}