/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.TToken;
import com.cyphail.lexer.TokenString;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class TokenParsersTest {

    // [MATCH, LPAREN, ID(p), RPAREN, EOF]
    private static InputTokens entrada() {
        return new InputTokens(List.of(
                new TokenString(TToken.MATCH, "MATCH"),
                new TokenString(TToken.LPAREN, "("),
                new TokenString(TToken.ID, "p"),
                new TokenString(TToken.RPAREN, ")"),
                new TokenString(TToken.EOF, "")));
    }

    @Test
    void reconoceElTokenEsperadoYAvanza() {
        var ok = assertInstanceOf(Ok.class, TokenParsers.Token(TToken.MATCH).parse(entrada()));

        assertEquals(new TokenString(TToken.MATCH, "MATCH"), ok.token());
        assertEquals(1, ((InputTokens) ok.rest()).index());
    }

    @Test
    void fallaSiElTipoNoCoincide() {
        var fail = assertInstanceOf(Fail.class, TokenParsers.Token(TToken.RPAREN).parse(entrada()));

        assertEquals("Expected RPAREN but found MATCH at token 0", fail.reason());
    }

    @Test
    void noConsumeNadaCuandoFalla() {
        // un parser que falla deja la entrada intacta para que otro pueda probar
        var entrada = entrada();
        TokenParsers.Token(TToken.RPAREN).parse(entrada);

        assertEquals(0, entrada.index());
    }

    @Test
    void fallaSinExplotarCuandoYaNoHayTokens() {
        var alFinal = new InputTokens(entrada().input(), 5);   // una posicion despues del EOF

        var fail = assertInstanceOf(Fail.class, TokenParsers.Token(TToken.EOF).parse(alFinal));
        assertEquals("Expected EOF but input ended at token 5", fail.reason());
    }

    @Test
    void variosTokensSeguidosAvanzanDeUnoEnUno() {
        var r1 = assertInstanceOf(Ok.class, TokenParsers.Token(TToken.MATCH).parse(entrada()));
        var r2 = assertInstanceOf(Ok.class,
                TokenParsers.Token(TToken.LPAREN).parse((InputTokens) r1.rest()));
        var r3 = assertInstanceOf(Ok.class,
                TokenParsers.Token(TToken.ID).parse((InputTokens) r2.rest()));

        assertEquals("p", ((TokenString) r3.token()).value());
        assertEquals(3, ((InputTokens) r3.rest()).index());
    }

    @Test
    void enlazaConElLexer() {
        // del texto a los tokens, y de los tokens al parser
        var tokens = assertInstanceOf(Ok.class, Lexers.tokenize("MATCH (p)"));

        @SuppressWarnings("unchecked")
        var entrada = new InputTokens((List<TokenString>) tokens.token());

        var ok = assertInstanceOf(Ok.class, TokenParsers.Token(TToken.MATCH).parse(entrada));
        assertEquals("MATCH", ((TokenString) ok.token()).value());
    }
}
