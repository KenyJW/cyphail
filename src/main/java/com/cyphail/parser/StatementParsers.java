/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.CreateStatement;
import com.cyphail.ast.DeleteStatement;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.Program;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.Statement;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;

import java.util.stream.Stream;

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

    // Retorna CreateStatement desde CREATE y los patrones separados por coma.
    public static Parser<InputTokens, Statement, String> createStatement() {
        var patterns = Parsers.SepBy(PatternParsers.nodePattern(),
                                     TokenParsers.Token(TToken.COMMA));

        var full = Parsers.And(TokenParsers.Token(TToken.CREATE), patterns);

        Parser<InputTokens, Statement, String> p =
                Parsers.Map(full, pair -> new CreateStatement(pair.second()));
        return p;
    }

    // Retorna DeleteStatement desde un DETACH opcional, DELETE y las expresiones.
    // TODO: el AST no guarda si venia DETACH; el caso 14 del SPEC lo va a necesitar.
    public static Parser<InputTokens, Statement, String> deleteStatement() {
        var targets = Parsers.SepBy(ExpressionParsers.operand(),
                                    TokenParsers.Token(TToken.COMMA));

        var full = Parsers.And(Parsers.Opt(TokenParsers.Token(TToken.DETACH)),
                               Parsers.And(TokenParsers.Token(TToken.DELETE), targets));

        Parser<InputTokens, Statement, String> p =
                Parsers.Map(full, pair -> new DeleteStatement(pair.second().second()));
        return p;
    }

    // Retorna Program desde las clausulas, el RETURN y el EOF.
    // El EOF es obligatorio: sin el, "MATCH (p) RETURN p basura" pasaria
    // dejando tokens sin leer.
    // TODO: el caso 13 del SPEC (DELETE sin RETURN) exigira hacerlo opcional.
    public static Parser<InputTokens, Program, String> program() {
        // El primero es un MATCH obligatorio ("todo query empieza con un match",
        // segun los casos de prueba). Exigirlo conserva su mensaje de error.
        var otherClauses = Parsers.Star(
                Parsers.Or(matchStatement(),
                        Parsers.Or(createStatement(), deleteStatement())));

        var clauses = Parsers.Map(Parsers.And(matchStatement(), otherClauses),
                pair -> Stream.concat(Stream.of(pair.first()), pair.second().stream()).toList());

        var items = Parsers.SepBy(returnItem(), TokenParsers.Token(TToken.COMMA));

        var returnPart = Parsers.And(TokenParsers.Token(TToken.RETURN), items);

        var full = Parsers.And(clauses,
                               Parsers.And(returnPart, TokenParsers.Token(TToken.EOF)));

        return Parsers.Map(full, pair -> new Program(pair.first(),
                                                     pair.second().first().second()));
    }
}
