/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Nota: integrante del grupo hasta P1.1; posteriormente salio del curso.
 */
package com.cyphail.frontend;

import com.cyphail.engine.FakeEngine;

public final class FrontendFactory {

    private FrontendFactory() {
    }

    public static RequestHandler createP11Handler() {
        return new FrontendRouter(new FakeEngine());
    }
}
