/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class GraphCatalog {

    private record Graph(String name, String description) {
    }

    private static final List<Graph> GRAPHS = List.of(
            new Graph("dragon", "BallZ"),
            new Graph("university", "UNA"),
            new Graph("basketball", "Lebron"),
            new Graph("career", "system engineer"),
            new Graph("movie", "Cars"),
            new Graph("dance", "Salsa")
    );

    private GraphCatalog() {
    }

    public static String use(String graphName){

if (graphName == null || graphName.isBlank()) {
            return listGraphs();
        }

else{
for (Graph graph : GRAPHS) {
    if (graph.name().equalsIgnoreCase(graphName)) {
        return "OK. \"%s\" graph available after %dms".formatted(graphName, 1);
    }
}
}
 
return "ERROR: %s not found".formatted(graphName); 
}

    private static String listGraphs() {
        StringBuilder sb = new StringBuilder();
        sb.append(pad("Graph", 12)).append("Description").append('\n');
        sb.append("-".repeat(36)).append('\n');
        for (Graph graph : GRAPHS) {
            sb.append(pad(graph.name(), 12)).append(graph.description()).append('\n');
        }
        sb.append('\n');
        sb.append("OK. Query available after ").append(fakeElapsedMs(3, 8)).append(" ms.");
        return sb.toString();
    }

    private static String pad(String text, int width) {
        return text + " ".repeat(Math.max(1, width - text.length()));
    }

    private static long fakeElapsedMs(long minInclusive, long maxInclusive) {
        return ThreadLocalRandom.current().nextLong(minInclusive, maxInclusive + 1);
    }
}
