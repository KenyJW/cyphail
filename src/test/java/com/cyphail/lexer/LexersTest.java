/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 */
package com.cyphail.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class LexersTest {

    private static InputString in(String text) {
        return new InputString(text, 0);
    }

    @Test
    void numberRecognizesDigitsAndAdvancesIndex() {
        var result = Lexers.Number().parse(in("123"));

        var ok = assertInstanceOf(Ok.class, result);
        assertEquals(new TokenString(TToken.NUM, "123"), ok.token());
        assertEquals(new InputString("123", 3), ok.rest());
    }

    @Test
    void numberFailsOnEmptyInput() {
        assertInstanceOf(Fail.class, Lexers.Number().parse(in("")));
    }

    @Test
    void numberSkipsLeadingWhitespace() {
        var ok = assertInstanceOf(Ok.class, Lexers.Number().parse(in("   42")));
        assertEquals(new TokenString(TToken.NUM, "42"), ok.token());
    }

    @Test
    void idRecognizesIdentifier() {
        var ok = assertInstanceOf(Ok.class, Lexers.Id().parse(in("abc")));
        assertEquals(new TokenString(TToken.ID, "abc"), ok.token());
    }

    @Test
    void idFailsOnNumber() {
        assertInstanceOf(Fail.class, Lexers.Id().parse(in("123")));
    }

    // ---------- Keyword (ejemplo resuelto) ----------

    @Test
    void keywordRecognizesMatch() {
        var ok = assertInstanceOf(Ok.class, Lexers.Keyword().parse(in("MATCH (p)")));
        assertEquals(new TokenString(TToken.MATCH, "MATCH"), ok.token());
        assertEquals(new InputString("MATCH (p)", 5), ok.rest());   // se detuvo antes del espacio
    }

    @Test
    void keywordIgnoresCaseButKeepsOriginalText() {
        var ok = assertInstanceOf(Ok.class, Lexers.Keyword().parse(in("match")));
        assertEquals(new TokenString(TToken.MATCH, "match"), ok.token());
    }

    @Test
    void keywordRejectsLongerWordsThanksToWordBoundary() {
        assertInstanceOf(Fail.class, Lexers.Keyword().parse(in("MATCHES")));
        assertInstanceOf(Fail.class, Lexers.Keyword().parse(in("ORDER")));
    }

    @Test
    void keywordMustGoBeforeIdInsideOr() {
        var correcto = Parsers.Or(Lexers.Keyword(), Lexers.Id());
        var alReves = Parsers.Or(Lexers.Id(), Lexers.Keyword());

        var bien = assertInstanceOf(Ok.class, correcto.parse(in("MATCH")));
        var mal = assertInstanceOf(Ok.class, alReves.parse(in("MATCH")));

        assertEquals(TToken.MATCH, ((TokenString) bien.token()).type());
        assertEquals(TToken.ID, ((TokenString) mal.token()).type());
    }

    @Test
    void orTriesTheSecondParserWhenTheFirstFails() {
        var idOrNum = Parsers.Or(Lexers.Id(), Lexers.Number());

        var id = assertInstanceOf(Ok.class, idOrNum.parse(in("abc")));
        var num = assertInstanceOf(Ok.class, idOrNum.parse(in("123")));

        assertEquals(TToken.ID, ((TokenString) id.token()).type());
        assertEquals(TToken.NUM, ((TokenString) num.token()).type());
        assertInstanceOf(Fail.class, idOrNum.parse(in("")));
    }
	
	    // ---------- Symbol ----------

    @Test
    void symbolRecognizesSingleCharacter() {
        var ok = assertInstanceOf(Ok.class, Lexers.Symbol().parse(in("(p)")));
        assertEquals(new TokenString(TToken.LPAREN, "("), ok.token());
        assertEquals(new InputString("(p)", 1), ok.rest());
    }

    @Test
    void symbolPrefersArrowOverDash() {
        var ok = assertInstanceOf(Ok.class, Lexers.Symbol().parse(in("->(q)")));
        assertEquals(new TokenString(TToken.ARROW_RIGHT, "->"), ok.token());
    }
	@Test
        void symbolPrefersDotDotOverDot() {
        var ok = assertInstanceOf(Ok.class, Lexers.Symbol().parse(in("..(q)")));
                assertEquals(new TokenString(TToken.DOTDOT, ".."), ok.token());
    }
	@Test
    void symbolPrefersLessEqualOverLess() {
		var ok = assertInstanceOf(Ok.class, Lexers.Symbol().parse(in("<= 5")));
        assertEquals(new TokenString(TToken.LE, "<="), ok.token());
    }

    @Test
    void symbolFailsOnLetters() {
        assertInstanceOf(Fail.class, Lexers.Symbol().parse(in("abc")));
    }
	
	
	    // ---------- StringLiteral ----------

    @Test
    void stringLiteralDropsQuotes() {
        // input: "Ana" con sus comillas; el token guarda solo Ana
        var ok = assertInstanceOf(Ok.class, Lexers.StringLiteral().parse(in("\"Ana\"")));
        assertEquals(new TokenString(TToken.STRING, "Ana"), ok.token());
        // se come las 5 posiciones, incluida la comilla que cierra
        assertEquals(new InputString("\"Ana\"", 5), ok.rest());
    }

    @Test
    void stringLiteralKeepsInnerSpaces() {
        // el \\s* solo come espacios ANTES de la comilla; los de adentro se conservan
        var ok = assertInstanceOf(Ok.class, Lexers.StringLiteral().parse(in("  \"Ana Maria\"")));
        assertEquals(new TokenString(TToken.STRING, "Ana Maria"), ok.token());
    }

    @Test
    void stringLiteralFailsWhenNotClosed() {
        assertInstanceOf(Fail.class, Lexers.StringLiteral().parse(in("\"Ana")));
    }
}
