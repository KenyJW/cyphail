/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Analyzer)
 */
package com.cyphail.analyzer;

import com.cyphail.ast.Program;
import com.cyphail.lexer.Ok;
import com.cyphail.parser.CyphailParser;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyzerTest {

    private static Optional<String> analyze(String source) {
        var ok = assertInstanceOf(Ok.class, CyphailParser.parse(source), "no parseo: " + source);
        return Analyzer.analyze((Program) ok.token());
    }

    private static void esCorrecto(String source) {
        assertEquals(Optional.empty(), analyze(source));
    }

    // ---------- los casos validos del profesor ----------

    @Test
    void caso1() {
        esCorrecto("""
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year""");
    }

    @Test
    void caso2() {
        esCorrecto("""
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages""");
    }

    @Test
    void caso3() {
        esCorrecto("""
                MATCH (a:Person:Employee {id: 1})
                WHERE a.age > 30
                RETURN a.name AS name,
                       a.age AS age""");
    }

    @Test
    void caso4() {
        esCorrecto("""
                MATCH (m:Movie), (p:Person)
                WHERE m.year > 2000
                RETURN m.title AS title,
                       p.name AS actor""");
    }

    @Test
    void caso5() {
        esCorrecto("""
                MATCH (m:Movie), (p:Person)
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name""");
    }

    @Test
    void caso6() {
        esCorrecto("""
                MATCH (m:
                Movie {year: 1999}), (p:Person {age: 40})
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name""");
    }

    @Test
    void caso7ElCreateDefineSuVariable() {
        esCorrecto("""
                MATCH (p:Person)
                WHERE p.age > 18
                CREATE (c:Certificate {issuedTo: "adult", year: 2026})
                RETURN p.name AS name,
                       p.age AS age""");
    }

    @Test
    void caso8UsaUnaVariableDefinidaEnElPatronAnterior() {
        // (o:Order {personId: p.id}) usa p, definida por el patron de la izquierda
        esCorrecto("""
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total""");
    }

    @Test
    void caso9ElDeleteUsaUnaVariableDefinida() {
        esCorrecto("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name""");
    }

    // ---------- los dos que deben fallar ----------

    @Test
    void caso10DetectaLaVariableDelWhere() {
        assertEquals(Optional.of("Undefined variable 'q'"), analyze("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name"""));
    }

    @Test
    void caso11DetectaElOrdenInvertido() {
        // p.id se usa en el primer patron, pero p se define en el segundo
        assertEquals(Optional.of("Undefined variable 'p'"), analyze("""
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name"""));
    }

    // ---------- otros casos ----------

    @Test
    void detectaLaVariableEnElReturn() {
        assertEquals(Optional.of("Undefined variable 'z'"),
                analyze("MATCH (p:Person) RETURN z.name"));
    }

    @Test
    void detectaLaVariableEnElDelete() {
        assertEquals(Optional.of("Undefined variable 'x'"),
                analyze("MATCH (p:Person) DELETE x RETURN p"));
    }

    @Test
    void reportaElPrimerErrorNoElUltimo() {
        var error = analyze("MATCH (p:Person) WHERE a.x > b.y RETURN p");
        assertTrue(error.isPresent());
        assertEquals(Optional.of("Undefined variable 'a'"), error);
    }

    @Test
    void unaVariableNoSeVeASiMismaEnSusPropiedades() {
        // (p {id: p.id}) usa p antes de definirla
        assertEquals(Optional.of("Undefined variable 'p'"),
                analyze("MATCH (p:Person {id: p.id}) RETURN p"));
    }
}
