/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.Direction;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.PatternStep;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.RelationshipPattern;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;
import com.cyphail.lexer.TokenString;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

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

    // ---------- 4d. Patron de relacion (P2) ----------

    // Retorna el cuerpo [r:TIPO {props}] como una funcion que espera la direccion.
    // La direccion no esta dentro de los corchetes: la aportan las flechas de afuera.
    // Opt(label()) y no Star(): una relacion tiene un solo tipo, un nodo varias etiquetas.
    private static Parser<InputTokens, Function<Direction, RelationshipPattern>, String> relationshipBody() {
        var typeAndProperties = Parsers.And(Parsers.Opt(label()),
                                            Parsers.Opt(propertyMap()));

        var inside = Parsers.And(variableName(), typeAndProperties);

        var insideAndClose = Parsers.And(inside, TokenParsers.Token(TToken.RBRACKET));

        var full = Parsers.And(TokenParsers.Token(TToken.LBRACKET), insideAndClose);

        return Parsers.Map(full, pair -> {
            var parts = pair.second().first();
            return direction -> new RelationshipPattern(
                    parts.first(),
                    parts.second().first(),
                    parts.second().second().orElse(List.of()),
                    direction);
        });
    }

    // Arma una de las tres formas: token de apertura, cuerpo, token de cierre.
    // Al cuerpo le aplica la direccion que corresponde a esas flechas.
    private static Parser<InputTokens, RelationshipPattern, String> arrows(
            TToken open, TToken close, Direction direction) {

        var bodyAndClose = Parsers.And(relationshipBody(), TokenParsers.Token(close));

        var full = Parsers.And(TokenParsers.Token(open), bodyAndClose);

        return Parsers.Map(full, pair -> pair.second().first().apply(direction));
    }

    // Retorna RelationshipPattern en cualquiera de sus tres formas.
    // Las tres se excluyen por sus tokens de los extremos, y cada una exige
    // los dos, asi que el orden dentro del Or no cambia el resultado.
    public static Parser<InputTokens, RelationshipPattern, String> relationshipPattern() {
        var out = arrows(TToken.DASH, TToken.ARROW_RIGHT, Direction.OUT);
        var in = arrows(TToken.ARROW_LEFT, TToken.DASH, Direction.IN);
        var none = arrows(TToken.DASH, TToken.DASH, Direction.NONE);

        return Parsers.Or(out, Parsers.Or(in, none));
    }

    // ---------- 4e. Camino completo (P2) ----------

    // Retorna PathPattern: un nodo inicial y cero o mas pasos.
    // Un nodo suelto (a) es un camino con la lista de pasos vacia.
    // El paso es seguro dentro de Star porque exige corchetes y parentesis:
    // nunca tiene exito consumiendo cero tokens.
    public static Parser<InputTokens, PathPattern, String> pathPattern() {
        var step = Parsers.Map(Parsers.And(relationshipPattern(), nodePattern()),
                pair -> new PatternStep(pair.first(), pair.second()));

        var full = Parsers.And(nodePattern(), Parsers.Star(step));

        return Parsers.Map(full, pair -> new PathPattern(pair.first(), pair.second()));
    }
}
