/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.NumberLiteral;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.Pair;
import com.cyphail.lexer.Parsers;
import com.cyphail.ast.VariableExpression;
import com.cyphail.lexer.TToken;
import com.cyphail.lexer.TokenString;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class CombinadoresTest {

    // [LPAREN, ID(p), RPAREN, NUM(42), EOF]
    private static InputTokens entrada() {
        return new InputTokens(List.of(
                new TokenString(TToken.LPAREN, "("),
                new TokenString(TToken.ID, "p"),
                new TokenString(TToken.RPAREN, ")"),
                new TokenString(TToken.NUM, "42"),
                new TokenString(TToken.EOF, "")));
    }

    private static InputTokens desde(int index) {
        return new InputTokens(entrada().input(), index);
    }

    // ---------- Map ----------

    @Test
    void mapCambiaElTipoDelResultado() {
        // el parser ya no devuelve un TokenString sino un String
        var parser = Parsers.Map(TokenParsers.Token(TToken.ID), TokenString::value);

        var ok = assertInstanceOf(Ok.class, parser.parse(desde(1)));
        assertEquals("p", ok.token());
    }

    @Test
    void mapNoConsumeNadaPorSuCuenta() {
        var sinMap = TokenParsers.Token(TToken.ID);
        var conMap = Parsers.Map(sinMap, TokenString::value);

        var a = assertInstanceOf(Ok.class, sinMap.parse(desde(1)));
        var b = assertInstanceOf(Ok.class, conMap.parse(desde(1)));

        assertEquals(((InputTokens) a.rest()).index(), ((InputTokens) b.rest()).index());
    }

    @Test
    void mapPropagaElFalloConSuRazon() {
        var parser = Parsers.Map(TokenParsers.Token(TToken.ID), TokenString::value);

        var fail = assertInstanceOf(Fail.class, parser.parse(desde(0)));   // en 0 hay un LPAREN
        assertEquals("Expected ID but found LPAREN at token 0", fail.reason());
    }

    @Test
    void mapPuedeConstruirNodosDelAst() {
        // asi es como el parser dejara de producir tokens y empezara a producir AST
        // Long.parseLong, no Integer: NumberLiteral guarda un long
        var numero = Parsers.Map(TokenParsers.Token(TToken.NUM),
                t -> new NumberLiteral(Long.parseLong(t.value())));

        var ok = assertInstanceOf(Ok.class, numero.parse(desde(3)));
        assertEquals(new NumberLiteral(42), ok.token());
    }

    @Test
    void mapAceptaNumerosMasGrandesQueUnInt() {
        // con Integer.parseInt esto lanzaria NumberFormatException
        var grande = new InputTokens(List.of(new TokenString(TToken.NUM, "3000000000")));

        var numero = Parsers.Map(TokenParsers.Token(TToken.NUM),
                t -> new NumberLiteral(Long.parseLong(t.value())));

        var ok = assertInstanceOf(Ok.class, numero.parse(grande));
        assertEquals(new NumberLiteral(3_000_000_000L), ok.token());
    }

    // ---------- And ----------

    @Test
    void andEncadenaLosDosParsers() {
        var parser = Parsers.And(TokenParsers.Token(TToken.LPAREN), TokenParsers.Token(TToken.ID));

        var ok = assertInstanceOf(Ok.class, parser.parse(entrada()));

        @SuppressWarnings("unchecked")
        var par = (Pair<TokenString, TokenString>) ok.token();
        assertEquals("(", par.first().value());
        assertEquals("p", par.second().value());

        // el rest es el que dejo el SEGUNDO parser
        assertEquals(2, ((InputTokens) ok.rest()).index());
    }

    @Test
    void andFallaSiFallaElPrimero() {
        var parser = Parsers.And(TokenParsers.Token(TToken.ID), TokenParsers.Token(TToken.LPAREN));

        var fail = assertInstanceOf(Fail.class, parser.parse(entrada()));
        assertEquals("Expected ID but found LPAREN at token 0", fail.reason());
    }

    @Test
    void andFallaConLaRazonDelSegundoYNoDejaNadaConsumido() {
        var parser = Parsers.And(TokenParsers.Token(TToken.LPAREN), TokenParsers.Token(TToken.NUM));
        var entrada = entrada();

        var fail = assertInstanceOf(Fail.class, parser.parse(entrada));

        // el primero si habia consumido, pero el fallo del segundo lo descarta todo
        assertEquals("Expected NUM but found ID at token 1", fail.reason());
        // y la entrada original sigue intacta: no hubo nada que deshacer
        assertEquals(0, entrada.index());
    }

    @Test
    void andYMapSeCombinan() {
        // ( seguido de un identificador, quedandose solo con el nombre
        var parser = Parsers.Map(
                Parsers.And(TokenParsers.Token(TToken.LPAREN),
                            TokenParsers.Token(TToken.ID)),
                par -> par.second().value());

        var ok = assertInstanceOf(Ok.class, parser.parse(entrada()));
        assertEquals("p", ok.token());
    }

    private static InputTokens tokens(String source) {
        var ok = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var list = (List<TokenString>) ok.token();
        return new InputTokens(list);
    }

    // ---------- 4a. Opt ----------

    @Test
    void optWrapsTheResultWhenItSucceeds() {
        var parser = Parsers.Opt(TokenParsers.Token(TToken.ID));

        var ok = assertInstanceOf(Ok.class, parser.parse(tokens("p")));
        assertEquals(Optional.of(new TokenString(TToken.ID, "p")), ok.token());
        assertEquals(1, ((InputTokens) ok.rest()).index());
    }

    @Test
    void optGivesEmptyAndConsumesNothingWhenItFails() {
        var parser = Parsers.Opt(TokenParsers.Token(TToken.ID));

        var ok = assertInstanceOf(Ok.class, parser.parse(tokens("42")));
        assertEquals(Optional.empty(), ok.token());
        assertEquals(0, ((InputTokens) ok.rest()).index());   // no avanzo
    }

    // ---------- 4a. SepBy ----------

    @Test
    void sepByWithASingleElement() {
        var parser = Parsers.SepBy(ExpressionParsers.operand(), TokenParsers.Token(TToken.COMMA));

        var ok = assertInstanceOf(Ok.class, parser.parse(tokens("a")));
        assertEquals(List.of(new VariableExpression("a")), ok.token());
    }

    @Test
    void sepByWithThreeElementsDropsTheSeparators() {
        var parser = Parsers.SepBy(ExpressionParsers.operand(), TokenParsers.Token(TToken.COMMA));

        var ok = assertInstanceOf(Ok.class, parser.parse(tokens("a, b, c")));
        assertEquals(List.of(new VariableExpression("a"),
                        new VariableExpression("b"),
                        new VariableExpression("c")),
                ok.token());
        assertEquals(5, ((InputTokens) ok.rest()).index());   // 3 ids + 2 comas
    }

    @Test
    void sepByFailsWhenThereIsNoFirstElement() {
        var parser = Parsers.SepBy(ExpressionParsers.operand(), TokenParsers.Token(TToken.COMMA));

        assertInstanceOf(Fail.class, parser.parse(tokens(",")));
    }

    @Test
    void sepByStopsAtATrailingSeparator() {
        // "a, b," -> Star(And(COMMA, operand)) falla en la ultima coma y para ahi
        var parser = Parsers.SepBy(ExpressionParsers.operand(), TokenParsers.Token(TToken.COMMA));

        var ok = assertInstanceOf(Ok.class, parser.parse(tokens("a, b,")));
        assertEquals(2, ((List<?>) ok.token()).size());
        assertEquals(3, ((InputTokens) ok.rest()).index());   // la coma sobrante queda sin consumir
    }

}
