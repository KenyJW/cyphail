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

    // ---------- variables de relacion (P2) ----------

    @Test
    void laVariableDeLaRelacionQuedaDefinida() {
        esCorrecto("MATCH (a)-[r]->(b) RETURN r");
    }

    @Test
    void tambienCuandoLaRelacionApuntaAlReves() {
        esCorrecto("MATCH (a)<-[r:KNOWS]-(b) RETURN r");
    }

    @Test
    void sePuedeLeerUnaPropiedadDeLaRelacion() {
        esCorrecto("MATCH (p)-[r:TRABAJA_EN {rol: 'Dev'}]->(e) RETURN r.rol");
    }

    @Test
    void laRelacionDeUnCreateTambienDefine() {
        esCorrecto("CREATE (a:Persona)-[r:TRABAJA_EN]->(e:Empresa) RETURN a, r, e");
    }

    @Test
    void sePuedeBorrarLaRelacion() {
        esCorrecto("MATCH (a)-[r:KNOWS]->(b) WHERE a.x > 1 DELETE r");
    }

    @Test
    void unaVariableInventadaSigueFallando() {
        // el arreglo define la relacion, no desactiva el chequeo
        assertEquals(Optional.of("Undefined variable 'q'"),
                analyze("MATCH (a)-[r]->(b) RETURN q"));
    }

    @Test
    void unaPropiedadDeLaRelacionNoPuedeUsarAlgoNoDefinido() {
        assertEquals(Optional.of("Undefined variable 'z'"),
                analyze("MATCH (a)-[r {rol: z.x}]->(b) RETURN r"));
    }

    @Test
    void unaRelacionSinVariableNoDefineNada() {
        assertEquals(Optional.of("Undefined variable 'r'"),
                analyze("MATCH (a)-[:KNOWS]->(b) RETURN r"));
    }
    // ---------- la variable de relacion no se repite ----------

    @Test
    void aRelationshipVariableCannotNameTwoEdges() {
        assertEquals(Optional.of("Relationship variable 'r' is used more than once"),
                analyze("MATCH (a)-[r]->(b), (c)-[r]->(d) RETURN a"));
    }

    @Test
    void theRepetitionAlsoCountsAcrossClauses() {
        // son dos clausulas, pero un mismo estatuto y un mismo grafo
        assertEquals(Optional.of("Relationship variable 'r' is used more than once"),
                analyze("MATCH (a)-[r]->(b) MATCH (c)-[r]->(d) RETURN a"));
    }

    @Test
    void aNodeVariableCanRepeat() {
        // repetir (b) es como se encadenan dos patrones: significa el mismo nodo
        esCorrecto("MATCH (a)-[r]->(b) MATCH (b)-[s]->(c) RETURN a");
    }

    @Test
    void twoRelationshipsInOnePathAreFine() {
        esCorrecto("MATCH (a)-[r]->(b)-[s]->(c) RETURN r, s");
    }

    @Test
    void relationshipsWithoutAVariableNeverCollide() {
        esCorrecto("MATCH (a)-[:KNOWS]->(b), (c)-[:KNOWS]->(d) RETURN a");
    }
    // ---------- NOT (P2) ----------

    @Test
    void notDoesNotHideAnUndefinedVariable() {
        assertEquals(Optional.of("Undefined variable 'q'"),
                analyze("MATCH (p) WHERE NOT q.x > 1 RETURN p"));
    }

    @Test
    void aNegatedConditionOverADefinedVariableIsCorrect() {
        esCorrecto("MATCH (p:Person) WHERE NOT p.active RETURN p.name");
    }
    // ---------- OR y parentesis (P2) ----------

    @Test
    void orChecksBothSides() {
        assertEquals(Optional.of("Undefined variable 'q'"),
                analyze("MATCH (p) WHERE p.x > 1 OR q.y > 2 RETURN p"));
    }

    @Test
    void aGroupedConditionIsCheckedToo() {
        assertEquals(Optional.of("Undefined variable 'z'"),
                analyze("MATCH (p) WHERE (p.x > 1 OR z.y > 2) AND NOT p.w >= 3 RETURN p"));
    }

    @Test
    void aFullBooleanConditionIsCorrect() {
        esCorrecto("MATCH (p:Person) "
                + "WHERE (p.age >= 18 OR NOT p.minor) AND p.age <= 65 "
                + "RETURN p.name AS name");
    }
    // ---------- una variable no es nodo y relacion a la vez (SPEC II.6) ----------

    @Test
    void aNodeVariableCannotAlsoNameARelationship() {
        // el ejemplo literal del SPEC
        assertEquals(Optional.of("Variable 'a' is used as a node and as a relationship"),
                analyze("MATCH (a:N), (b)-[a]->(c) RETURN b"));
    }

    @Test
    void theOrderOfTheTwoUsesDoesNotMatter() {
        // ahora la relacion aparece primero y el nodo despues
        assertEquals(Optional.of("Variable 'a' is used as a node and as a relationship"),
                analyze("MATCH (b)-[a]->(c), (a:N) RETURN b"));
    }

    @Test
    void theClashIsAlsoDetectedAcrossClauses() {
        assertEquals(Optional.of("Variable 'r' is used as a node and as a relationship"),
                analyze("MATCH (x)-[r]->(y) CREATE (r:Persona) RETURN x"));
    }

    @Test
    void distinctNamesForNodesAndRelationshipsAreFine() {
        esCorrecto("MATCH (a:N), (b)-[r]->(c) RETURN a, b, c, r");
    }

    @Test
    void aNodeNameRepeatedAsANodeIsStillFine() {
        // el chequeo nuevo no debe confundirse con el join de nodos
        esCorrecto("MATCH (a)-[r]->(b) MATCH (b)-[s]->(a) RETURN a");
    }
    // ---------- las variables no empiezan con __ (SPEC II.7) ----------

    @Test
    void aNodeVariableCannotStartWithTwoUnderscores() {
        // el ejemplo literal del SPEC
        assertEquals(Optional.of("Variable '__person' cannot start with '__'"),
                analyze("MATCH (__person:Persona) RETURN __person"));
    }

    @Test
    void aRelationshipVariableCannotEither() {
        assertEquals(Optional.of("Variable '__r' cannot start with '__'"),
                analyze("MATCH (a)-[__r]->(b) RETURN a"));
    }

    @Test
    void anAliasCannotEither() {
        // el alias tambien llega al codegen, asi que tambien choca
        assertEquals(Optional.of("Variable '__name' cannot start with '__'"),
                analyze("MATCH (p) RETURN p.name AS __name"));
    }

    @Test
    void oneUnderscoreIsAllowed() {
        // la regla son DOS: "_p" es un nombre valido
        esCorrecto("MATCH (_p:Persona) RETURN _p");
    }

    @Test
    void underscoresInTheMiddleAreAllowed() {
        esCorrecto("MATCH (p_1:Persona) RETURN p_1 AS n__ame");
    }

    @Test
    void aPropertyCanStartWithTwoUnderscores() {
        // la restriccion es sobre variables, no sobre propiedades: una
        // propiedad se transpila a un atomo, no a una variable de Prolog
        esCorrecto("MATCH (p {__oculto: 1}) RETURN p.__interno");
    }
}
