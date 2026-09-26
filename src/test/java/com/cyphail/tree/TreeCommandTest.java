/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeCommandTest {

    private static String tree(String query) {
        return TreeCommand.run(query).replace(System.lineSeparator(), "\n");
    }

    @Test
    void caso1MuestraElArbolCompleto() {
        assertEquals("""
                Query
                  matchPart
                    nodePattern
                      variable
                        m
                      labels
                        Movie
                  returnPart
                    returnItem
                      propertyAccess
                        variable
                          m
                        property
                          title
                    returnItem
                      propertyAccess
                        variable
                          m
                        property
                          year
                      alias
                        year
                """, tree("MATCH (m:Movie) RETURN m.title, m.year AS year"));
    }

    @Test
    void loAusenteSeOmite() {
        // (a) sin etiquetas ni propiedades: no aparecen esos nodos
        var salida = tree("MATCH (a) RETURN a");

        assertTrue(salida.contains("nodePattern"));
        assertFalse(salida.contains("labels"));
        assertFalse(salida.contains("properties"));
        assertFalse(salida.contains("alias"));
        assertFalse(salida.contains("where"));
    }

    @Test
    void elPatronSinVariableNoLlevaNodoVariable() {
        var salida = tree("MATCH (:Person) RETURN 1");

        assertTrue(salida.contains("labels"));
        assertFalse(salida.contains("variable"));
    }

    @Test
    void muestraEtiquetasMultiplesYPropiedades() {
        var salida = tree("MATCH (a:Person:Employee {id: 1}) RETURN a");

        // sin bloque de texto: """ recorta la sangria y aqui la sangria es el dato
        assertTrue(salida.contains("      labels\n        Person\n        Employee\n"),
                salida);
        assertTrue(salida.contains("      properties\n        property\n          key\n            id\n"),
                salida);
    }

    @Test
    void elErrorDeSintaxisNoMuestraArbol() {
        var salida = tree("MATCH (p RETURN p");

        assertEquals("ERROR: Expected RPAREN but found RETURN at token 3", salida);
        assertFalse(salida.contains("Query"));
    }

    @Test
    void laVariableNoDefinidaSeAgregaAlFinalDelArbol() {
        // el arbol si se muestra: el parser funciono, el error es semantico
        var salida = tree("MATCH (p:Person) WHERE q.age > 60 RETURN q AS name");

        assertTrue(salida.startsWith("Query"));
        assertTrue(salida.endsWith("ERROR: Undefined variable 'q'"));
    }

    @Test
    void sinConsultaPideUna() {
        assertTrue(tree("").startsWith("ERROR: .tree needs a query"));
    }

    @Test
    void createYDeleteTienenSuPropiaParte() {
        var salida = tree("MATCH (p:Person) CREATE (c:Certificate {year: 2026}) DELETE p RETURN p");

        assertTrue(salida.contains("createPart"));
        assertTrue(salida.contains("deletePart"));
    }
}
