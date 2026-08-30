/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

public final class ReplHelp {

    private ReplHelp() {
    }

    public static String text() {
        return """
                Cyphail REPL commands:
                  .help          helps you
                  .about         Show information about the project
                  .use           List available graphs
                  .use <name>    Connect to a graph
                  .exit          Exit

                Helping you!!!
                Type for example: 
                CREATE (you:Person {name:"Loria", age:666})
                to run a fake query""";
    }
}
