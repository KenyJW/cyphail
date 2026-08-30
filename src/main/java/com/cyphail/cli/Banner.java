/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

public final class Banner {

    private static final String GROUP_LABEL = "04-1pm";

    private Banner() {
    }

    public static String welcome() {
        return """
                Welcome to Cyphail-%s v.0.1. August 2026. ESCINF/UNA EIF400-II-2026
                Visit www.whatiscyphail.com for more information
                Type ".help" for more information and commands
                Type ".exit" to quit""".formatted(GROUP_LABEL);
    }
}
