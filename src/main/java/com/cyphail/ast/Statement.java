/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import java.util.List;

public sealed interface Statement permits MatchStatement, CreateStatement, DeleteStatement,
                                    SetStatement, RemoveStatement {

    // Retorna los caminos que escribe la clausula. Solo MATCH y CREATE traen
    // patrones; las otras tres nombran variables ya definidas, asi que no
    // aportan ninguno. Vive aqui y no en cada clase que lo necesite para que
    // una clausula nueva obligue a decidirlo en un solo lugar.
    default List<PathPattern> paths() {
        return switch (this) {
            case MatchStatement match -> match.patterns();
            case CreateStatement create -> create.patterns();
            case DeleteStatement delete -> List.of();
            case SetStatement set -> List.of();
            case RemoveStatement remove -> List.of();
        };
    }
}
