/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.ReturnItem;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Ok;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CyphailParserTest {

    @Test
    void caso1DelSpecDaElProgramCompleto() {
        var ok = assertInstanceOf(Ok.class, CyphailParser.parse("MATCH (p:Person) RETURN p.name"));

        assertEquals(new Program(
                        List.of(new MatchStatement(
                                List.of(new NodePattern(Optional.of("p"), List.of("Person"), List.of())),
                                Optional.empty())),
                        List.of(new ReturnItem(new PropertyAccessExpression("p", "name"),
                                Optional.empty()))),
                ok.token());
    }

    @Test
    void errorDelLexerLlegaConSuMensaje() {
        var fail = assertInstanceOf(Fail.class, CyphailParser.parse("MATCH (p) @ RETURN p"));

        assertTrue(((String) fail.reason()).contains("@"),
                "el mensaje deberia mencionar el caracter: " + fail.reason());
    }

    @Test
    void returnSinItemsFalla() {
        assertInstanceOf(Fail.class, CyphailParser.parse("MATCH (p) RETURN"));
    }

    @Test
    void textoVacioFallaSinLanzarExcepcion() {
        assertInstanceOf(Fail.class, CyphailParser.parse(""));
    }

    @Test
    void matchSinReturnFalla() {
        assertInstanceOf(Fail.class, CyphailParser.parse("MATCH (p:Person)"));
    }

    @Test
    void caso3ConSaltosDeLinea() {
        var ok = assertInstanceOf(Ok.class, CyphailParser.parse("""
                MATCH (p:Person)
                WHERE p.age > 25
                RETURN p.name"""));

        var program = (Program) ok.token();
        var match = (MatchStatement) program.clauses().get(0);
        assertTrue(match.where().isPresent());
    }
}
