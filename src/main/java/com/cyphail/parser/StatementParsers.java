/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.Program;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.Statement;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;

// Parsers de clausulas y del programa completo.
public final class StatementParsers {

    private StatementParsers() {
    }

    // Retorna ReturnItem desde una expresion y un AS opcional. Descarta el AS.
    public static Parser<InputTokens, ReturnItem, String> returnItem() {
        var alias = Parsers.Map(Parsers.And(TokenParsers.Token(TToken.AS),
                                            TokenParsers.Token(TToken.ID)),
                                pair -> pair.second().value());

        var full = Parsers.And(ExpressionParsers.operand(), Parsers.Opt(alias));

        return Parsers.Map(full, pair -> new ReturnItem(pair.first(), pair.second()));
    }

    // Retorna MatchStatement desde MATCH, patrones separados por coma y WHERE opcional.
    // El tipo se declara como Statement para que Program pueda guardarlo en su lista.
    public static Parser<InputTokens, Statement, String> matchStatement() {
        var patterns = Parsers.SepBy(PatternParsers.nodePattern(),
                                     TokenParsers.Token(TToken.COMMA));

        var where = Parsers.Opt(
                Parsers.Map(Parsers.And(TokenParsers.Token(TToken.WHERE),
                                        ExpressionParsers.comparison()),
                            pair -> pair.second()));

        var full = Parsers.And(TokenParsers.Token(TToken.MATCH),
                               Parsers.And(patterns, where));

        Parser<InputTokens, Statement, String> p =
                Parsers.Map(full, pair -> new MatchStatement(pair.second().first(),
                                                             pair.second().second()));
        return p;
    }

    // Retorna Program desde las clausulas, el RETURN y el EOF.
    // El EOF es obligatorio: sin el, "MATCH (p) RETURN p basura" pasaria
    // dejando tokens sin leer.
    // TODO: el caso 13 del SPEC (DELETE sin RETURN) exigira hacerlo opcional.
    public static Parser<InputTokens, Program, String> program() {
        // Some y no Star: sin al menos una clausula, el error del MATCH mal
        // formado se perdia y el mensaje senalaba el token 0.
        var clauses = Parsers.Some(matchStatement());

        var items = Parsers.SepBy(returnItem(), TokenParsers.Token(TToken.COMMA));

        var returnPart = Parsers.And(TokenParsers.Token(TToken.RETURN), items);

        var full = Parsers.And(clauses,
                               Parsers.And(returnPart, TokenParsers.Token(TToken.EOF)));

        return Parsers.Map(full, pair -> new Program(pair.first(),
                                                     pair.second().first().second()));
    }
}
