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
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;

import java.util.List;
import java.util.Optional;
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
        var patterns = Parsers.SepBy(PatternParsers.pathPattern(),
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
        var patterns = Parsers.SepBy(PatternParsers.pathPattern(),
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

    // Retorna Program desde las clausulas, el RETURN opcional y el EOF.
    // El EOF es obligatorio: sin el, "MATCH (p) RETURN p basura" pasaria
    // dejando tokens sin leer.
    public static Parser<InputTokens, Program, String> program() {
        // Cualquier clausula puede ir primero: el SPEC de P2 admite CREATE
        // sin MATCH previo. Some y no Star: un programa vacio no es valido.
        //
        // Sin Label, y con matchStatement() AL FINAL a proposito: un Or que
        // falla reporta la razon de su ULTIMA alternativa, y la del match es
        // casi siempre la util. Las tres se distinguen por su primer token,
        // asi que el orden no cambia que reconoce, solo que mensaje da.
        var clause = Parsers.Or(createStatement(),
                Parsers.Or(deleteStatement(), matchStatement()));

        var clauses = Parsers.Some(clause);

        var items = Parsers.SepBy(returnItem(), TokenParsers.Token(TToken.COMMA));

        // El RETURN es opcional desde P2: "CREATE (a:Persona);" es valido.
        // Pero si la palabra RETURN esta, las proyecciones son obligatorias:
        // con un Opt normal, "RETURN" solo retrocederia y el error diria
        // "Expected EOF" en vez de senalar la expresion que falta.
        var returned = Parsers.Map(
                Parsers.And(TokenParsers.Token(TToken.RETURN), items),
                pair -> Optional.of(pair.second()));

        Parser<InputTokens, Optional<List<ReturnItem>>, String> returnPart =
                source -> TokenParsers.Token(TToken.RETURN).parse(source) instanceof Ok
                        ? returned.parse(source)
                        : new Ok<>(Optional.empty(), source);

        // El ';' final es opcional: en el REPL de P2 cierra el estatuto, pero
        // un archivo o un .tree pueden traerlo o no. Se descarta al construir.
        var end = Parsers.And(Parsers.Opt(TokenParsers.Token(TToken.SEMICOLON)),
                              TokenParsers.Token(TToken.EOF));

        var full = Parsers.And(clauses, Parsers.And(returnPart, end));

        return Parsers.Map(full, pair -> new Program(
                pair.first(),
                pair.second().first().orElse(List.of())));
    }
}
