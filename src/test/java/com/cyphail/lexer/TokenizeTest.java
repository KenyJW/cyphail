/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Lexer)
 */
package com.cyphail.lexer;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenizeTest {

    // los tipos de la lista, para comparar sin escribir cada texto
    private static List<TToken> tipos(String source) {
        var ok = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var tokens = (List<TokenString>) ok.token();
        return tokens.stream().map(TokenString::type).toList();
    }

    private static List<String> textos(String source) {
        var ok = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var tokens = (List<TokenString>) ok.token();
        return tokens.stream().map(TokenString::value).toList();
    }

    // ---------- Star ----------

    @Test
    void starNuncaFallaYDevuelveListaVaciaConTextoVacio() {
        var ok = assertInstanceOf(Ok.class,
                Parsers.Star(Lexers.anyToken()).parse(new InputString("", 0)));
        assertEquals(List.of(), ok.token());
    }

    @Test
    void starSeDetieneEnLoQueNoReconoce() {
        // '@' no lo reconoce ningun lexer: Star para ahi y devuelve lo que llevaba
        var ok = assertInstanceOf(Ok.class,
                Parsers.Star(Lexers.anyToken()).parse(new InputString("MATCH (p) @ RETURN p", 0)));
        @SuppressWarnings("unchecked")
        var tokens = (List<TokenString>) ok.token();
        assertEquals(4, tokens.size());
        assertEquals(9, ((InputString) ok.rest()).index());
    }

    // ---------- tokenize: casos de uso del SPEC ----------

    @Test
    void caso1MatchSimple() {
        assertEquals(List.of(TToken.MATCH, TToken.LPAREN, TToken.ID, TToken.COLON, TToken.ID,
                        TToken.RPAREN, TToken.RETURN, TToken.ID, TToken.DOT, TToken.ID, TToken.EOF),
                tipos("MATCH (p:Person) RETURN p.name"));
    }

    @Test
    void caso2MatchConRelacion() {
        assertEquals(List.of("MATCH", "(", "p", ":", "Person", ")", "-", "[", ":", "FOLLOWS", "]",
                        "->", "(", "q", ":", "Person", ")", "RETURN", "p", ".", "name", ""),
                textos("MATCH (p:Person)-[:FOLLOWS]->(q:Person) RETURN p.name"));
    }

    @Test
    void caso3FiltroConComparacion() {
        assertTrue(tipos("MATCH (p) WHERE p.age > 25 RETURN p").contains(TToken.GT));
        assertTrue(tipos("MATCH (p) WHERE p.age >= 25 RETURN p").contains(TToken.GE));
    }

    @Test
    void caso9CreateConPropiedades() {
        assertEquals(List.of("CREATE", "(", "a", ":", "Person", "{", "name", ":", "Ana Maria",
                        ",", "age", ":", "30", "}", ")", ""),
                textos("CREATE (a:Person {name: \"Ana Maria\", age: 30})"));
    }

    @Test
    void caso7CaminoDeLongitudVariable() {
        assertTrue(tipos("MATCH (a)-[:FOLLOWS*1..3]->(b) RETURN b").containsAll(
                List.of(TToken.STAR, TToken.DOTDOT, TToken.ARROW_RIGHT)));
    }

    @Test
    void caso14DetachDelete() {
        assertEquals(List.of(TToken.MATCH, TToken.LPAREN, TToken.ID, TToken.RPAREN,
                        TToken.DETACH, TToken.DELETE, TToken.ID, TToken.EOF),
                tipos("MATCH (p) DETACH DELETE p"));
    }

    // ---------- tokenize: casos limite ----------

    @Test
    void textoVacioSoloDaEof() {
        assertEquals(List.of(TToken.EOF), tipos(""));
        assertEquals(List.of(TToken.EOF), tipos("   \n  "));
    }

    @Test
    void caracterDesconocidoDaFail() {
        var fail = assertInstanceOf(Fail.class, Lexers.tokenize("MATCH (p) @ RETURN p"));
        assertEquals("Unrecognized character '@' at position 9", fail.reason());
    }

    @Test
    void stringSinCerrarDaFail() {
        assertInstanceOf(Fail.class, Lexers.tokenize("CREATE (a {name: \"Ana})"));
    }
}
