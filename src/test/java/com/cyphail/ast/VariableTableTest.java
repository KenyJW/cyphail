/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.TokenString;
import com.cyphail.parser.InputTokens;
import com.cyphail.parser.StatementParsers;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

class VariableTableTest {

    private static VariableTable withThree() {
        var orden = new LinkedHashMap<String, VariableKind>();
        orden.put("p", VariableKind.NODE);
        orden.put("r", VariableKind.RELATIONSHIP);
        orden.put("e", VariableKind.NODE);
        return new VariableTable(orden);
    }

    @Test
    void keepsTheOrderOfFirstAppearance() {
        // con Map.copyOf este orden cambiaba en cada ejecucion de la JVM
        assertEquals(List.of("p", "r", "e"), List.copyOf(withThree().variables().keySet()));
    }

    @Test
    void remembersWhatEachVariableNames() {
        assertEquals(VariableKind.RELATIONSHIP, withThree().variables().get("r"));
        assertEquals(VariableKind.NODE, withThree().variables().get("p"));
    }

    @Test
    void theTableCannotBeModified() {
        assertThrows(UnsupportedOperationException.class,
                () -> withThree().variables().put("x", VariableKind.NODE));
    }

    @Test
    void changingTheSourceMapDoesNotChangeTheTable() {
        var fuente = new LinkedHashMap<String, VariableKind>();
        fuente.put("p", VariableKind.NODE);
        var tabla = new VariableTable(fuente);

        fuente.put("q", VariableKind.NODE);

        assertEquals(1, tabla.variables().size());
    }
    // ---------- la tabla que se arma desde el AST ----------

    // Pasa por el lexer y el parser de verdad: lo que se prueba es la tabla
    // del programa que el usuario escribio, no un AST armado a mano.
    private static VariableTable tableOf(String source) {
        var lexed = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var list = (List<TokenString>) lexed.token();

        var parsed = assertInstanceOf(Ok.class,
                StatementParsers.program().parse(new InputTokens(list)));

        return VariableTable.from((Program) parsed.token());
    }

    private static List<String> namesOf(VariableTable table) {
        return List.copyOf(table.variables().keySet());
    }

    @Test
    void aRelationshipVariableIsNotMarkedAsANode() {
        var table = tableOf("MATCH (p:Persona)-[r:TRABAJA_EN]->(e:Empresa) RETURN p");

        assertEquals(VariableKind.NODE, table.variables().get("p"));
        assertEquals(VariableKind.RELATIONSHIP, table.variables().get("r"));
        assertEquals(VariableKind.NODE, table.variables().get("e"));
    }

    @Test
    void theOrderIsTheOneWritten() {
        // la relacion va en el medio, no al final
        assertEquals(List.of("p", "r", "e"),
                namesOf(tableOf("MATCH (p)-[r]->(e) RETURN p")));
    }

    @Test
    void aRepeatedVariableKeepsItsFirstPlace() {
        // "e" ya estaba en la posicion 3: volver a nombrarla no la mueve al final
        assertEquals(List.of("p", "r", "e", "s"),
                namesOf(tableOf("MATCH (p)-[r]->(e) MATCH (e)-[s]->(p) RETURN p")));
    }

    @Test
    void createAlsoDefinesVariables() {
        assertEquals(List.of("a", "w", "b"),
                namesOf(tableOf("CREATE (a:Persona)-[w:CONOCE]->(b:Persona);")));
    }

    @Test
    void aClauseWithoutPatternsAddsNothing() {
        // SET nombra "p", pero no la define: la tabla ya la traia del MATCH
        assertEquals(List.of("p"), namesOf(tableOf("MATCH (p) SET p.stock = 3;")));
    }
}
