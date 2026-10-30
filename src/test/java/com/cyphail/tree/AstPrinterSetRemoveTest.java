/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (.tree)
 */
package com.cyphail.tree;

import com.cyphail.ast.AndExpression;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.DecimalLiteral;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.RemoveStatement;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.SetItem;
import com.cyphail.ast.SetStatement;
import com.cyphail.ast.Statement;
import com.cyphail.ast.VariableExpression;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

// El parser todavia no produce SET ni REMOVE, asi que el AST se arma a mano:
// estas pruebas fijan como los imprime .tree cuando el parser llegue.
class AstPrinterSetRemoveTest {

    private static final PropertyAccessExpression PRECIO =
            new PropertyAccessExpression("p", "precio");

    private static Program programWith(Statement update) {
        var match = new MatchStatement(
                List.of(new PathPattern(
                        new NodePattern(Optional.of("p"), List.of("Producto"), List.of()),
                        List.of())),
                Optional.empty());

        return new Program(List.of(match, update),
                List.of(new ReturnItem(new VariableExpression("p"), Optional.empty())));
    }

    @Test
    void setPrintsEachAssignment() {
        var salida = AstPrinter.print(programWith(new SetStatement(List.of(
                new SetItem(PRECIO, new DecimalLiteral(19.99)),
                new SetItem(new PropertyAccessExpression("p", "stock"), new NumberLiteral(3))))));

        assertTrue(salida.contains("Set:{"), salida);
        assertTrue(salida.contains("{(. p precio) 19.99}"), salida);
        assertTrue(salida.contains("{(. p stock) 3}"), salida);
    }

    @Test
    void removePrintsItsTargets() {
        var salida = AstPrinter.print(programWith(
                new RemoveStatement(List.of(new PropertyAccessExpression("p", "discount")))));

        assertTrue(salida.contains("Remove:{"), salida);
        assertTrue(salida.contains("Targets: [ (. p discount) ]"), salida);
    }

    @Test
    void setAndRemoveLiveInsideUpdates() {
        var salida = AstPrinter.print(programWith(
                new RemoveStatement(List.of(new PropertyAccessExpression("p", "discount")))));

        assertTrue(salida.contains("Updates:["), salida);
        assertTrue(salida.indexOf("Updates:[") < salida.indexOf("Remove:{"), salida);
    }

    // ---------- lo demas que agrego P2 al impresor ----------

    @Test
    void andIsPrintedInPrefix() {
        assertEquals("(and (> (. p stock) 0) (= (. p precio) 19.99))",
                AstPrinter.expression(new AndExpression(
                        new ComparisonExpression(new PropertyAccessExpression("p", "stock"),
                                ComparisonOperator.GREATER_THAN, new NumberLiteral(0)),
                        new ComparisonExpression(PRECIO,
                                ComparisonOperator.EQUALS, new DecimalLiteral(19.99)))));
    }

    @Test
    void theEqualsSymbolIsPrinted() {
        assertEquals("(= (. p precio) 19.99)",
                AstPrinter.expression(new ComparisonExpression(
                        PRECIO, ComparisonOperator.EQUALS, new DecimalLiteral(19.99))));
    }

    @Test
    void decimalsKeepTheirDot() {
        assertEquals("19.99", AstPrinter.expression(new DecimalLiteral(19.99)));
    }
}
