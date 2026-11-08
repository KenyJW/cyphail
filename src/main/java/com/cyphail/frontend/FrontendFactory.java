/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor original: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Modificado por: Jose Moya Perez (Integracion SWI-Prolog)
 Nota: Sebastian fue integrante del grupo hasta P1.1;
 posteriormente salio del curso.
 */
package com.cyphail.frontend;

import com.cyphail.engine.CompilerEngineBroker;
import com.cyphail.engine.SwiPrologEngineBroker;

import java.io.IOException;
import java.time.Duration;

public final class FrontendFactory {

    private static final Duration ENGINE_TIMEOUT =
            Duration.ofSeconds(10);

    private FrontendFactory() {
    }

    public static FrontendSession createP2Session()
            throws IOException {

        String executable = System.getProperty(
                "cyphail.swipl",
                "swipl"
        );

        SwiPrologEngineBroker engine =
                SwiPrologEngineBroker.start(
                        executable,
                        ENGINE_TIMEOUT
                );

        RequestHandler handler = new FrontendRouter(
                new CompilerEngineBroker(engine)
        );

        return new FrontendSession(
                handler,
                engine
        );
    }
}