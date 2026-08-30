/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Sebastian Ramirez Calderon (Frontend / Router / Handlers)
 Nota: integrante del grupo hasta P1.1; posteriormente salio del curso.
 */
package com.cyphail.frontend;

@FunctionalInterface
public interface RequestHandler {
    CyphailResponse handle(String input);
}
