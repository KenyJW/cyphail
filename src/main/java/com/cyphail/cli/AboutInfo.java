/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

public final class AboutInfo {

    private AboutInfo() {
    }

    public static String text() {
        return """
                Cyphail v0.1 - EIF400-II-2026-CLoria
                Escuela de Informatica, Universidad Nacional (UNA), Costa Rica
                Group 4

                Authors:
                  Kenny Jimenez Wang (Coordinator) - CLI / REPL
                    ID: 119300403 - kenny.jimenez.wang@est.una.ac.cr
                  Jose Moya Perez - Engine Broker / Fake Engine
                    ID: 605010893 - jose.moya.perez@est.una.ac.cr
                  Sebastian Ramirez Calderon - Frontend / Router / Handlers
                    ID: 118180269 - left the course after P1.1""";
    }
}
