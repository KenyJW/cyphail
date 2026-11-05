/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.BooleanLiteral;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.DecimalLiteral;
import com.cyphail.ast.AndExpression;
import com.cyphail.ast.Expression;
import com.cyphail.ast.NotExpression;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.OrExpression;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.Parser;
import com.cyphail.lexer.Parsers;
import com.cyphail.lexer.TToken;

import java.util.List;

// Convierte tokens en nodos de com.cyphail.ast.
public final class ExpressionParsers {

    private ExpressionParsers() {
    }

    // ---------- 3a. Literales ----------

    // Retorna NumberLiteral desde un token NUM.
    // El tipo se declara como Expression: sin eso, Or no unifica los literales.
    public static Parser<InputTokens, Expression, String> number() {
        Parser<InputTokens, Expression, String> p =
                Parsers.Map(TokenParsers.Token(TToken.NUM),
                        t -> new NumberLiteral(Long.parseLong(t.value())));
        return p;
    }

    // Retorna StringLiteral desde un token STRING.
    public static Parser<InputTokens, Expression, String> string() {
        Parser<InputTokens, Expression, String> p =
                Parsers.Map(TokenParsers.Token(TToken.STRING),
                        t -> new StringLiteral(t.value()));
        return p;
    }

    // Retorna BooleanLiteral desde TRUE o FALSE.
    public static Parser<InputTokens, Expression, String> bool() {
        Parser<InputTokens, Expression, String> yes =
                Parsers.Map(TokenParsers.Token(TToken.TRUE), t -> new BooleanLiteral(true));

        Parser<InputTokens, Expression, String> no =
                Parsers.Map(TokenParsers.Token(TToken.FALSE), t -> new BooleanLiteral(false));

        return Parsers.Or(yes, no);
    }

    // Retorna DecimalLiteral desde un token DECIMAL: 19.99
    // Va aparte de number() porque NumberLiteral guarda un long.
    public static Parser<InputTokens, Expression, String> decimal() {
        Parser<InputTokens, Expression, String> p =
                Parsers.Map(TokenParsers.Token(TToken.DECIMAL),
                        t -> new DecimalLiteral(Double.parseDouble(t.value())));
        return p;
    }

    // Prueba los cuatro literales.
    public static Parser<InputTokens, Expression, String> literal() {
        return Parsers.Or(number(), Parsers.Or(decimal(), Parsers.Or(string(), bool())));
    }

    // ---------- 3b. Variable y acceso a propiedad ----------

    // Retorna VariableExpression desde un token ID.
    public static Parser<InputTokens, Expression, String> variable() {
        Parser<InputTokens, Expression, String> p =
                Parsers.Map(TokenParsers.Token(TToken.ID),
                        t -> new VariableExpression(t.value()));
        return p;
    }

    // Retorna PropertyAccessExpression desde ID DOT ID.
    // El And anidado da Pair<ID, Pair<DOT, ID>>; Map lo aplana y descarta el DOT.
    public static Parser<InputTokens, Expression, String> propertyAccess() {
        var dotAndName = Parsers.And(TokenParsers.Token(TToken.DOT),
                                     TokenParsers.Token(TToken.ID));

        var full = Parsers.And(TokenParsers.Token(TToken.ID), dotAndName);

        Parser<InputTokens, Expression, String> p =
                Parsers.Map(full,
                        pair -> new PropertyAccessExpression(pair.first().value(),
                                                             pair.second().second().value()));
        return p;
    }

    // El mismo acceso a propiedad, pero con el tipo concreto.
    // SET y REMOVE lo necesitan asi: sus records guardan
    // PropertyAccessExpression, no cualquier Expression.
    public static Parser<InputTokens, PropertyAccessExpression, String> propertyTarget() {
        var dotAndName = Parsers.And(TokenParsers.Token(TToken.DOT),
                                     TokenParsers.Token(TToken.ID));

        var full = Parsers.And(TokenParsers.Token(TToken.ID), dotAndName);

        return Parsers.Map(full,
                pair -> new PropertyAccessExpression(pair.first().value(),
                                                     pair.second().second().value()));
    }

    // Cualquier operando. propertyAccess va primero: las dos alternativas
    // empiezan con ID, y la mas larga debe intentarse antes.
    public static Parser<InputTokens, Expression, String> operand() {
        return TokenParsers.Label(
                Parsers.Or(propertyAccess(), Parsers.Or(variable(), literal())),
                "an expression");
    }

    // ---------- 3c. Grupo entre parentesis (P2) ----------

    // Retorna lo que haya dentro de "( ... )", descartando los parentesis.
    // Adentro se vuelve al nivel mas flojo: un grupo puede traer un OR.
    private static Parser<InputTokens, Expression, String> group() {
        var innerAndClose = Parsers.And(lazyExpression(),
                                        TokenParsers.Token(TToken.RPAREN));

        var full = Parsers.And(TokenParsers.Token(TToken.LPAREN), innerAndClose);

        return Parsers.Map(full, pair -> pair.second().first());
    }

    // El grupo vuelve a llamar al nivel mas externo, asi que necesita el
    // mismo retraso que la negacion: sin esto la construccion no termina.
    private static Parser<InputTokens, Expression, String> lazyExpression() {
        return source -> expression().parse(source);
    }

    // ---------- 3d. Comparacion ----------

    // Retorna ComparisonOperator desde LT, GT, NEQ, EQ, LE o GE.
    // No produce Expression: el operador no es un nodo del AST, es parte de uno.
    public static Parser<InputTokens, ComparisonOperator, String> operator() {
        Parser<InputTokens, ComparisonOperator, String> lt =
                Parsers.Map(TokenParsers.Token(TToken.LT), t -> ComparisonOperator.LESS_THAN);

        Parser<InputTokens, ComparisonOperator, String> gt =
                Parsers.Map(TokenParsers.Token(TToken.GT), t -> ComparisonOperator.GREATER_THAN);

        Parser<InputTokens, ComparisonOperator, String> neq =
                Parsers.Map(TokenParsers.Token(TToken.NEQ), t -> ComparisonOperator.NOT_EQUALS);

        Parser<InputTokens, ComparisonOperator, String> eq =
                Parsers.Map(TokenParsers.Token(TToken.EQ), t -> ComparisonOperator.EQUALS);

        Parser<InputTokens, ComparisonOperator, String> le =
                Parsers.Map(TokenParsers.Token(TToken.LE), t -> ComparisonOperator.LESS_OR_EQUAL);

        Parser<InputTokens, ComparisonOperator, String> ge =
                Parsers.Map(TokenParsers.Token(TToken.GE), t -> ComparisonOperator.GREATER_OR_EQUAL);

        // El orden no importa: el lexer ya decidio entre "<" y "<=", asi que
        // cada alternativa espera un token distinto.
        return Parsers.Or(lt, Parsers.Or(gt, Parsers.Or(neq,
                          Parsers.Or(eq, Parsers.Or(le, ge)))));
    }

    // Retorna ComparisonExpression desde operand operator operand.
    private static Parser<InputTokens, Expression, String> fullComparison() {
        var operatorAndRight = Parsers.And(operator(), operand());

        var full = Parsers.And(operand(), operatorAndRight);

        Parser<InputTokens, Expression, String> p =
                Parsers.Map(full,
                        pair -> new ComparisonExpression(pair.first(),
                                                         pair.second().first(),
                                                         pair.second().second()));
        return p;
    }

    // Expresion con o sin comparacion. La completa va primero, o el operando
    // solo ganaria y dejaria el operador sin consumir.
    public static Parser<InputTokens, Expression, String> comparison() {
        var plain = Parsers.Or(fullComparison(), operand());

        // El grupo no se ofrece como alternativa, se decide por el primer
        // token. Puesto en un Or, su razon taparia la del operando o al
        // contrario: un Or reporta la razon de su ULTIMA alternativa, y aqui
        // las dos hacen falta. Asi "(p.x > 1" dice "Expected RPAREN" y
        // "WHERE RETURN" dice "an expression", cada una en su caso.
        return source -> TokenParsers.Token(TToken.LPAREN).parse(source) instanceof Ok
                ? group().parse(source)
                : plain.parse(source);
    }

    // ---------- 3e. Negacion (P2) ----------

    // Retorna NotExpression desde NOT seguido de otra negacion, o la
    // comparacion sola si no hay NOT. Va entre el AND y la comparacion:
    // "NOT a AND b" niega solo la a.
    public static Parser<InputTokens, Expression, String> negation() {
        var full = Parsers.And(TokenParsers.Token(TToken.NOT), lazyNegation());

        Parser<InputTokens, Expression, String> negated =
                Parsers.Map(full, pair -> new NotExpression(pair.second()));

        return Parsers.Or(negated, comparison());
    }

    // El nivel se referencia a si mismo para admitir "NOT NOT x".
    // Construirlo de una vez seria recursion infinita: un parser es una
    // funcion entrada -> resultado, asi que este envoltorio retrasa la
    // construccion hasta que alguien de verdad parsea.
    private static Parser<InputTokens, Expression, String> lazyNegation() {
        return source -> negation().parse(source);
    }

    // ---------- 3f. Conjuncion (P2) ----------

    // Una o mas negaciones unidas por AND. Con una sola, devuelve esa
    // negacion y no envuelve nada: "a > 1" no es un AndExpression.
    // Asocia a la derecha: "a AND b AND c" da (and a (and b c)).
    public static Parser<InputTokens, Expression, String> andLevel() {
        var parts = Parsers.SepBy(negation(), TokenParsers.Token(TToken.AND));

        return Parsers.Map(parts, ExpressionParsers::joinWithAnd);
    }

    private static Expression joinWithAnd(List<Expression> parts) {
        return parts.size() == 1
                ? parts.get(0)
                : new AndExpression(parts.get(0),
                                    joinWithAnd(parts.subList(1, parts.size())));
    }
    // ---------- 3g. Disyuncion (P2) ----------

    // El nivel mas flojo, y el que todos llaman cuando quieren "una expresion".
    // Una o mas conjunciones unidas por OR. Llamar aqui a andLevel() es lo que
    // fija la precedencia: "a OR b AND c" da (or a (and b c)) y no al reves,
    // porque el AND se resuelve completo antes de que el OR vea sus partes.
    public static Parser<InputTokens, Expression, String> expression() {
        var parts = Parsers.SepBy(andLevel(), TokenParsers.Token(TToken.OR));

        return Parsers.Map(parts, ExpressionParsers::joinWithOr);
    }

    private static Expression joinWithOr(List<Expression> parts) {
        return parts.size() == 1
                ? parts.get(0)
                : new OrExpression(parts.get(0),
                                   joinWithOr(parts.subList(1, parts.size())));
    }
}
