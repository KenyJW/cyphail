/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.lexer.Fail;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// El SPEC pide que los errores de sintaxis se muestren como mensaje.
// Estas pruebas fijan los mensajes que ve el usuario en el REPL.
class ErrorMessagesTest {

    private static String reasonOf(String source) {
        var fail = assertInstanceOf(Fail.class, CyphailParser.parse(source));
        return (String) fail.reason();
    }

    @Test
    void faltaLaExpresionDespuesDeReturn() {
        // sin Label decia "Expected FALSE", la ultima alternativa del Or de literales
        assertEquals("Expected an expression but found EOF at token 5",
                reasonOf("MATCH (p) RETURN"));
    }

    @Test
    void faltaElOperandoDerechoDeLaComparacion() {
        assertEquals("Expected RETURN but found GT at token 8",
                reasonOf("MATCH (p) WHERE p.age > RETURN p"));
    }

    @Test
    void parentesisSinCerrarSenalaElTokenCorrecto() {
        // sin Some decia "Expected RETURN but found MATCH at token 0"
        assertEquals("Expected RPAREN but found RETURN at token 3",
                reasonOf("MATCH (p RETURN p"));
    }

    @Test
    void faltaElParentesisDeApertura() {
        assertEquals("Expected LPAREN but found ID at token 1",
                reasonOf("MATCH p:Person) RETURN p"));
    }

    @Test
    void basuraAlFinal() {
        assertEquals("Expected EOF but found ID at token 6",
                reasonOf("MATCH (p) RETURN p basura"));
    }

    @Test
    void caracterDesconocidoLoReportaElLexer() {
        assertEquals("Unrecognized character '@' at position 9",
                reasonOf("MATCH (p) @ RETURN p"));
    }

    @Test
    void textoVacioPideUnMatch() {
        assertEquals("Expected MATCH but found EOF at token 0", reasonOf(""));
    }
}
