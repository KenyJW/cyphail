/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import com.cyphail.analyzer.Analyzer;
import com.cyphail.ast.Program;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Ok;
import com.cyphail.parser.CyphailParser;

// El comando .tree del REPL: parsea, y si puede, muestra el AST.
// Si el Analyzer encuentra una variable no definida, lo agrega al final:
// el arbol sigue siendo util para ver que el parser si funciono.
public final class TreeCommand {

    private TreeCommand() {
    }

    public static String run(String query) {
        if (query == null || query.isBlank()) {
            return "ERROR: .tree needs a query. Example: .tree MATCH (m:Movie) RETURN m.title";
        }

        return switch (CyphailParser.parse(query)) {

            case Fail<?, ?, ?> fail -> "ERROR: " + fail.reason();

            case Ok<?, ?, ?> ok -> {
                var program = (Program) ok.token();
                var tree = AstPrinter.print(program);

                yield Analyzer.analyze(program)
                        .map(error -> tree + "ERROR: " + error)
                        .orElse(tree);
            }
        };
    }
}
