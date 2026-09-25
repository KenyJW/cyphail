/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.CreateStatement;
import com.cyphail.ast.DeleteStatement;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.PropertyEntry;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;
import com.cyphail.lexer.Ok;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// Casos de prueba de referencia del profesor, copiados tal cual de
// EIF400-II-2026-SPEC-Sprint-P1-Casos de Prueba-CLoria.md, saltos de linea incluidos.
// El caso 10 parsea: detectar que q no esta definida es analisis semantico.
class CasosProfesorTest {

    private static Program parse(String source) {
        var ok = assertInstanceOf(Ok.class, CyphailParser.parse(source),
                "no parseo: " + source);
        return (Program) ok.token();
    }

    private static MatchStatement matchOf(Program p) {
        return (MatchStatement) p.clauses().get(0);
    }

    // CASO 1 Un patron y proyeccion con un alias.
    @Test
    void profesorCaso1() {
        var p = parse("""
                MATCH (m:Movie)
                RETURN m.title,
                       m.year AS year""");

        assertEquals(List.of(new NodePattern(Optional.of("m"), List.of("Movie"), List.of())),
                matchOf(p).patterns());
        assertEquals(Optional.empty(), matchOf(p).where());
        assertEquals(List.of(
                        new ReturnItem(new PropertyAccessExpression("m", "title"), Optional.empty()),
                        new ReturnItem(new PropertyAccessExpression("m", "year"), Optional.of("year"))),
                p.returnItems());
    }

    // CASO 2 Un patron, where y proyeccion con un alias. Comparacion.
    @Test
    void profesorCaso2() {
        var p = parse("""
                MATCH (b:Book)
                WHERE b.pages < 300
                RETURN b.title,
                       b.pages AS totalPages""");

        assertEquals(Optional.of(new ComparisonExpression(
                        new PropertyAccessExpression("b", "pages"),
                        ComparisonOperator.LESS_THAN,
                        new NumberLiteral(300))),
                matchOf(p).where());
        assertEquals(Optional.of("totalPages"), p.returnItems().get(1).alias());
    }

    // CASO 3 Un patron con etiquetas multiples
    @Test
    void profesorCaso3() {
        var p = parse("""
                MATCH (a:Person:Employee {id: 1})
                WHERE a.age > 30
                RETURN a.name AS name,
                       a.age AS age""");

        assertEquals(List.of(new NodePattern(Optional.of("a"),
                        List.of("Person", "Employee"),
                        List.of(new PropertyEntry("id", new NumberLiteral(1))))),
                matchOf(p).patterns());
        assertEquals(2, p.returnItems().size());
    }

    // CASO 4 Doble Patron (desconectados), proyecciones con aliases
    @Test
    void profesorCaso4() {
        var p = parse("""
                MATCH (m:Movie), (p:Person)
                WHERE m.year > 2000
                RETURN m.title AS title,
                       p.name AS actor""");

        assertEquals(2, matchOf(p).patterns().size());
        assertEquals(Optional.of(new ComparisonExpression(
                        new PropertyAccessExpression("m", "year"),
                        ComparisonOperator.GREATER_THAN,
                        new NumberLiteral(2000))),
                matchOf(p).where());
    }

    // CASO 5 Doble patron atributos distintos
    @Test
    void profesorCaso5() {
        var p = parse("""
                MATCH (m:Movie), (p:Person)
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name""");

        // la comparacion es entre dos accesos a propiedad, sin literales
        assertEquals(Optional.of(new ComparisonExpression(
                        new PropertyAccessExpression("m", "year"),
                        ComparisonOperator.NOT_EQUALS,
                        new PropertyAccessExpression("p", "age"))),
                matchOf(p).where());
    }

    // CASO 6 Doble patron c/u con atributo
    @Test
    void profesorCaso6() {
        // ojo: el salto de linea esta DENTRO del patron, entre "(m:" y "Movie"
        var p = parse("""
                MATCH (m:
                Movie {year: 1999}), (p:Person {age: 40})
                WHERE m.year <> p.age
                RETURN m.title AS title,
                       p.name AS name""");

        assertEquals(List.of(
                        new NodePattern(Optional.of("m"), List.of("Movie"),
                                List.of(new PropertyEntry("year", new NumberLiteral(1999)))),
                        new NodePattern(Optional.of("p"), List.of("Person"),
                                List.of(new PropertyEntry("age", new NumberLiteral(40))))),
                matchOf(p).patterns());
    }

    // CASO 8 Match con patron de property expresion simple
    @Test
    void profesorCaso8() {
        var p = parse("""
                MATCH (p:Person {id: 1}), (o:Order {personId: p.id})
                RETURN p.name AS name,
                       o.total AS total""");

        // el valor de la propiedad es un acceso a propiedad, no un literal
        assertEquals(List.of(new PropertyEntry("personId",
                        new PropertyAccessExpression("p", "id"))),
                matchOf(p).patterns().get(1).properties());
    }

    // CASO 10 Deteccion de variables no definidas.
    // PARSEA correctamente: q es sintacticamente valida. Que q nunca se haya
    // declarado es un error semantico, y hace falta un analizador del AST.
    @Test
    void profesorCaso10ParsePeroFaltaElAnalisisSemantico() {
        var p = parse("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE q.age > 60
                RETURN q AS name""");

        assertEquals(2, matchOf(p).patterns().size());
    }

    // CASO 7 Match un patron y create
    @Test
    void profesorCaso7() {
        var p = parse("""
                MATCH (p:Person)
                WHERE p.age > 18
                CREATE (c:Certificate {issuedTo: "adult", year: 2026})
                RETURN p.name AS name,
                       p.age AS age""");

        assertEquals(2, p.clauses().size());
        var create = assertInstanceOf(CreateStatement.class, p.clauses().get(1));
        assertEquals(List.of(new NodePattern(Optional.of("c"), List.of("Certificate"),
                        List.of(new PropertyEntry("issuedTo", new StringLiteral("adult")),
                                new PropertyEntry("year", new NumberLiteral(2026))))),
                create.patterns());
    }

    // CASO 9 Match create delete
    @Test
    void profesorCaso9() {
        var p = parse("""
                MATCH (p:Person), (o:Order {personId: p.id, status: "cancelled"})
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name""");

        assertEquals(3, p.clauses().size());
        assertInstanceOf(MatchStatement.class, p.clauses().get(0));
        assertInstanceOf(CreateStatement.class, p.clauses().get(1));

        var delete = assertInstanceOf(DeleteStatement.class, p.clauses().get(2));
        assertEquals(List.of(new VariableExpression("o")), delete.targets());
    }

    // CASO 11 Orden invertido variables no definidas (p.id antes de p).
    // Sintacticamente es identico al 9: el problema es semantico.
    @Test
    void profesorCaso11ParsePeroFaltaElAnalisisSemantico() {
        var p = parse("""
                MATCH (o:Order {personId: p.id, status: "cancelled"}), (p:Person)
                WHERE p.age > 60
                CREATE (a:Archive {id: o.id, name: "retired", year: 2026})
                DELETE o
                RETURN p.name AS name""");

        assertEquals(3, p.clauses().size());
    }
}
