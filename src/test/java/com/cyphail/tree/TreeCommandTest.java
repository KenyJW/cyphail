/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreeCommandTest {

    private static String tree(String query) {
        return TreeCommand.run(query).replace(System.lineSeparator(), "\n");
    }

    // ---------- expresiones en pre-orden ----------

    @Test
    void accesoAPropiedad() {
        assertEquals("(. m year)",
                AstPrinter.expression(new PropertyAccessExpression("m", "year")));
    }

    @Test
    void comparacionConSusTresOperadores() {
        var left = new PropertyAccessExpression("m", "year");
        var right = new NumberLiteral(1990);

        assertEquals("(> (. m year) 1990)",
                AstPrinter.expression(new ComparisonExpression(left, ComparisonOperator.GREATER_THAN, right)));
        assertEquals("(< (. m year) 1990)",
                AstPrinter.expression(new ComparisonExpression(left, ComparisonOperator.LESS_THAN, right)));
        assertEquals("(<> (. m year) 1990)",
                AstPrinter.expression(new ComparisonExpression(left, ComparisonOperator.NOT_EQUALS, right)));
    }

    @Test
    void literalesYVariables() {
        assertEquals("1990", AstPrinter.expression(new NumberLiteral(1990)));
        assertEquals("\"Ana\"", AstPrinter.expression(new StringLiteral("Ana")));
        assertEquals("m", AstPrinter.expression(new VariableExpression("m")));
    }

    // ---------- el ejemplo del SPEC ----------

    @Test
    void ejemploDelSpec() {
        assertEquals("""
                Query{
                  Match: {
                    Patterns: [
                      PatternNode:{
                        var: m
                        labels: [ Movie ]
                        properties: []
                      }
                    ]
                  }
                  Where: {
                    Expr: (> (. m year) 1990)
                  }
                  Updates:[]
                  Return:{
                    Projection:{
                      Items:[
                        {as (. m title) title}
                        {as (. m year) year}
                      ]
                      Modifiers:[]
                    }
                  }
                }
                """, tree("""
                MATCH (m:Movie)
                WHERE m.year > 1990
                RETURN m.title AS title,
                       m.year AS year"""));
    }

    // ---------- bloques ----------

    @Test
    void sinWhereSeOmiteElBloque() {
        var salida = tree("MATCH (m:Movie) RETURN m.title");

        assertFalse(salida.contains("Where"));
        assertTrue(salida.contains("Updates:[]"));
    }

    @Test
    void sinAliasElItemVaSolo() {
        assertTrue(tree("MATCH (m:Movie) RETURN m.title").contains("{(. m title)}"));
    }

    @Test
    void variasEtiquetasYPropiedades() {
        var salida = tree("MATCH (a:Person:Employee {id: 1}) RETURN a");

        assertTrue(salida.contains("labels: [ Person, Employee ]"), salida);
        assertTrue(salida.contains("properties: [ {id 1} ]"), salida);
    }

    @Test
    void createYDeleteVanEnUpdates() {
        var salida = tree("MATCH (p:Person) CREATE (a:Archive {year: 2026}) DELETE p RETURN p");

        assertTrue(salida.contains("Updates:["), salida);
        assertTrue(salida.contains("Create:{"), salida);
        assertTrue(salida.contains("Delete:{"), salida);
        assertTrue(salida.contains("Targets: [ p ]"), salida);
    }

    @Test
    void patronSinVariableNoMuestraVar() {
        var salida = tree("MATCH (:Person) RETURN 1");

        assertTrue(salida.contains("labels: [ Person ]"));
        assertFalse(salida.contains("var:"));
    }

    // ---------- errores ----------

    @Test
    void elErrorDeSintaxisNoMuestraArbol() {
        var salida = tree("MATCH (p RETURN p");

        assertEquals("ERROR: Expected RPAREN but found RETURN at token 3", salida);
        assertFalse(salida.contains("Query{"));
    }

    @Test
    void laVariableNoDefinidaSeReportaEnIngles() {
        // el SPEC lo pide al generar la salida; el arbol se muestra igual,
        // porque prueba que el parser si funciono
        var salida = tree("MATCH (p:Person) WHERE q.age > 60 RETURN q AS name");

        assertTrue(salida.startsWith("Query{"));
        assertTrue(salida.endsWith("ERROR: Undefined variable 'q'"));
    }

    @Test
    void unaPropiedadNoEsUnaVariableNoDefinida() {
        // el SPEC lo aclara: title en m.title no es variable no definida
        assertFalse(tree("MATCH (m:Movie) RETURN m.title AS title").contains("ERROR"));
    }

    @Test
    void sinConsultaPideUna() {
        assertTrue(tree("").startsWith("ERROR: .tree needs a query"));
    }
}
