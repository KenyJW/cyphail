/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Integracion Frontend / SWI-Prolog)
 */
package com.cyphail.frontend;

import java.io.IOException;
import java.util.Objects;

public final class FrontendSession implements AutoCloseable {

    private final RequestHandler handler;
    private final AutoCloseable engine;

    public FrontendSession(
            RequestHandler handler,
            AutoCloseable engine
    ) {
        this.handler = Objects.requireNonNull(
                handler,
                "handler cannot be null"
        );
        this.engine = Objects.requireNonNull(
                engine,
                "engine cannot be null"
        );
    }

    public RequestHandler handler() {
        return handler;
    }

    @Override
    public void close() throws IOException {
        try {
            engine.close();
        } catch (IOException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IOException(
                    "Could not close the SWI-Prolog engine.",
                    exception
            );
        }
    }
}