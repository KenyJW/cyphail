/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.NodePattern;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;
import com.cyphail.lexer.TokenString;

import java.util.List;
import java.util.Optional;

// Parsers de patrones: propiedades y, mas adelante, el node pattern completo.
public final class PatternParsers {

    private PatternParsers() {
    }

    // Retorna PropertyEntry desde ID COLON valor. Descarta el COLON.
    public static Parser<InputTokens, PropertyEntry, String> propertyEntry() {
        var colonAndValue = Parsers.And(TokenParsers.Token(TToken.COLON),
                                        ExpressionParsers.operand());

        var full = Parsers.And(TokenParsers.Token(TToken.ID), colonAndValue);

        return Parsers.Map(full,
                pair -> new PropertyEntry(pair.first().value(),
                                          pair.second().second()));
    }

    // Retorna la lista de propiedades entre llaves: {name: "Ana", age: 30}
    // Exige al menos una: {} todavia no se acepta.
    public static Parser<InputTokens, List<PropertyEntry>, String> propertyMap() {
        var entries = Parsers.SepBy(propertyEntry(), TokenParsers.Token(TToken.COMMA));

        var entriesAndClose = Parsers.And(entries, TokenParsers.Token(TToken.RBRACE));

        var full = Parsers.And(TokenParsers.Token(TToken.LBRACE), entriesAndClose);

        return Parsers.Map(full, pair -> pair.second().first());
    }

    // ---------- 4c. Node pattern ----------

    // Retorna el nombre de la etiqueta desde COLON ID. Descarta el COLON.
    private static Parser<InputTokens, String, String> label() {
        var full = Parsers.And(TokenParsers.Token(TToken.COLON),
                               TokenParsers.Token(TToken.ID));

        return Parsers.Map(full, pair -> pair.second().value());
    }

    // Retorna el nombre de la variable, o vacio si no hay.
    private static Parser<InputTokens, Optional<String>, String> variableName() {
        return Parsers.Opt(Parsers.Map(TokenParsers.Token(TToken.ID), TokenString::value));
    }

    // Retorna NodePattern desde el interior de los parentesis.
    // Las propiedades ausentes quedan como List.of(), no como Optional.
    private static Parser<InputTokens, NodePattern, String> body() {
        var labelsAndProperties = Parsers.And(Parsers.Star(label()),
                                              Parsers.Opt(propertyMap()));

        var full = Parsers.And(variableName(), labelsAndProperties);

        return Parsers.Map(full, pair -> new NodePattern(
                pair.first(),
                pair.second().first(),
                pair.second().second().orElse(List.of())));
    }

    // Retorna NodePattern desde LPAREN cuerpo RPAREN.
    public static Parser<InputTokens, NodePattern, String> nodePattern() {
        var bodyAndClose = Parsers.And(body(), TokenParsers.Token(TToken.RPAREN));

        var full = Parsers.And(TokenParsers.Token(TToken.LPAREN), bodyAndClose);

        // Sin Label: nodePattern es una secuencia, no una alternativa, y sus
        // errores internos ("Expected RPAREN...") son mas precisos que una etiqueta.
        return Parsers.Map(full, pair -> pair.second().first());
    }
}
