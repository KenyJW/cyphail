/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (Parser)
 */
package com.cyphail.parser;

import com.cyphail.ast.BooleanLiteral;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.StringLiteral;
import com.cyphail.ast.VariableExpression;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Lexers;
import com.cyphail.lexer.Ok;
import com.cyphail.lexer.TokenString;
import com.cyphail.ast.AndExpression;
import com.cyphail.ast.NotExpression;
import com.cyphail.ast.OrExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class ExpressionParsersTest {

    // Del texto a la entrada del parser, pasando por el lexer.
    private static InputTokens tokens(String source) {
        var ok = assertInstanceOf(Ok.class, Lexers.tokenize(source));
        @SuppressWarnings("unchecked")
        var list = (List<TokenString>) ok.token();
        return new InputTokens(list);
    }

    // ---------- 3a ----------

    @Test
    void numberGivesNumberLiteral() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.literal().parse(tokens("42")));
        assertEquals(new NumberLiteral(42), ok.token());
    }

    @Test
    void stringGivesStringLiteralWithoutQuotes() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.literal().parse(tokens("\"Ana\"")));
        assertEquals(new StringLiteral("Ana"), ok.token());
    }

    @Test
    void trueAndFalseGiveBooleanLiteral() {
        var yes = assertInstanceOf(Ok.class, ExpressionParsers.literal().parse(tokens("TRUE")));
        var no = assertInstanceOf(Ok.class, ExpressionParsers.literal().parse(tokens("false")));

        assertEquals(new BooleanLiteral(true), yes.token());
        assertEquals(new BooleanLiteral(false), no.token());
    }

    @Test
    void literalFailsOnIdentifier() {
        assertInstanceOf(Fail.class, ExpressionParsers.literal().parse(tokens("p")));
    }

    // ---------- 3b ----------

    @Test
    void identifierGivesVariableExpression() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.operand().parse(tokens("p")));
        assertEquals(new VariableExpression("p"), ok.token());
    }

    @Test
    void dotGivesPropertyAccessExpression() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.operand().parse(tokens("p.age")));
        assertEquals(new PropertyAccessExpression("p", "age"), ok.token());
    }

    @Test
    void propertyAccessConsumesThreeTokens() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.operand().parse(tokens("p.age")));
        assertEquals(3, ((InputTokens) ok.rest()).index());   // ID DOT ID, antes del EOF
    }

    @Test
    void orderMatters() {
        // con Or(variable, propertyAccess) esto daria VariableExpression y dejaria ".age"
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.operand().parse(tokens("p.age")));
        assertInstanceOf(PropertyAccessExpression.class, ok.token());
    }

    @Test
    void variableAloneStillWorksWithoutDot() {
        // propertyAccess falla por falta de DOT y el Or pasa a variable
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.operand().parse(tokens("p RETURN")));
        assertEquals(new VariableExpression("p"), ok.token());
        assertEquals(1, ((InputTokens) ok.rest()).index());
    }

    @Test
    void operandAlsoAcceptsLiterals() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.operand().parse(tokens("25")));
        assertEquals(new NumberLiteral(25), ok.token());
    }

    // ---------- 3c ----------

    @Test
    void comparisonBuildsComparisonExpression() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("p.age > 25")));

        assertEquals(new ComparisonExpression(new PropertyAccessExpression("p", "age"),
                        ComparisonOperator.GREATER_THAN,
                        new NumberLiteral(25)),
                ok.token());
    }

    @Test
    void comparisonWorksBetweenTwoVariables() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("a <> c")));

        assertEquals(new ComparisonExpression(new VariableExpression("a"),
                        ComparisonOperator.NOT_EQUALS,
                        new VariableExpression("c")),
                ok.token());
    }

    @Test
    void comparisonFallsBackToASingleOperand() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("p")));
        assertEquals(new VariableExpression("p"), ok.token());
    }

    @Test
    void incompleteComparisonDoesNotFailHere() {
        // "p.age >" sin operando derecho: fullComparison falla, el Or cae a operand(),
        // y el > queda sin consumir. El error aparece mas arriba, cuando MATCH espere RETURN.
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("p.age >")));

        assertEquals(new PropertyAccessExpression("p", "age"), ok.token());
        assertEquals(3, ((InputTokens) ok.rest()).index());   // ID DOT ID; el GT sigue ahi
    }

    @Test
    void operatorGivesTheEnumValue() {
        var lt = assertInstanceOf(Ok.class, ExpressionParsers.operator().parse(tokens("<")));
        var gt = assertInstanceOf(Ok.class, ExpressionParsers.operator().parse(tokens(">")));
        var neq = assertInstanceOf(Ok.class, ExpressionParsers.operator().parse(tokens("<>")));

        assertEquals(ComparisonOperator.LESS_THAN, lt.token());
        assertEquals(ComparisonOperator.GREATER_THAN, gt.token());
        assertEquals(ComparisonOperator.NOT_EQUALS, neq.token());
    }

    // ---------- el operador = (P2) ----------

    @Test
    void equalsIsAComparisonOperator() {
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.comparison().parse(tokens("a.age = b.age")));

        assertEquals(new ComparisonExpression(
                        new PropertyAccessExpression("a", "age"),
                        ComparisonOperator.EQUALS,
                        new PropertyAccessExpression("b", "age")),
                ok.token());
    }

    @Test
    void equalsDoesNotStealTheOtherOperators() {
        var menor = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("a.x < 1")));
        var distinto = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("a.x <> 1")));

        assertEquals(ComparisonOperator.LESS_THAN,
                ((ComparisonExpression) menor.token()).operator());
        assertEquals(ComparisonOperator.NOT_EQUALS,
                ((ComparisonExpression) distinto.token()).operator());
    }
    // ---------- 3c-bis. Los operadores con igual (P2) ----------

    @Test
    void theOperatorsWithEqualAreRecognized() {
        var le = assertInstanceOf(Ok.class, ExpressionParsers.operator().parse(tokens("<=")));
        var ge = assertInstanceOf(Ok.class, ExpressionParsers.operator().parse(tokens(">=")));

        assertEquals(ComparisonOperator.LESS_OR_EQUAL, le.token());
        assertEquals(ComparisonOperator.GREATER_OR_EQUAL, ge.token());
    }

    @Test
    void theLexerDecidesBetweenLessAndLessOrEqual() {
        // "<=" no se lee como "<" seguido de "=": es un solo token
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("m.year <= 2000")));

        assertEquals(ComparisonOperator.LESS_OR_EQUAL,
                ((ComparisonExpression) ok.token()).operator());
    }

    @Test
    void aComparisonWithGreaterOrEqual() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.comparison().parse(tokens("p.age >= 18")));

        assertEquals(new ComparisonExpression(new PropertyAccessExpression("p", "age"),
                                              ComparisonOperator.GREATER_OR_EQUAL,
                                              new NumberLiteral(18)),
                ok.token());
    }
    // ---------- 3d. Negacion (P2) ----------

    @Test
    void notNegatesTheComparison() {
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.negation().parse(tokens("NOT p.active")));

        assertEquals(new NotExpression(new PropertyAccessExpression("p", "active")), ok.token());
    }

    @Test
    void withoutNotTheLevelGivesTheComparisonAlone() {
        // el nivel es transparente: no envuelve nada si no hay NOT
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.negation().parse(tokens("p.age > 18")));

        assertInstanceOf(ComparisonExpression.class, ok.token());
    }

    @Test
    void notCanBeRepeated() {
        // aqui es donde hace falta que el nivel se referencie a si mismo
        var ok = assertInstanceOf(Ok.class, ExpressionParsers.negation().parse(tokens("NOT NOT p.x")));

        assertEquals(new NotExpression(new NotExpression(new PropertyAccessExpression("p", "x"))),
                ok.token());
    }

    @Test
    void notBindsTighterThanAnd() {
        // "NOT a AND b" niega solo la a: el AND queda por fuera
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.expression().parse(tokens("NOT p.a > 1 AND p.b < 2")));

        var and = assertInstanceOf(AndExpression.class, ok.token());
        assertInstanceOf(NotExpression.class, and.left());
        assertInstanceOf(ComparisonExpression.class, and.right());
    }
    // ---------- 3g. Disyuncion y parentesis (P2) ----------

    @Test
    void andBindsTighterThanOr() {
        // "a OR b AND c" es (or a (and b c)): el AND se resuelve completo
        // antes de que el OR vea sus partes
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.expression().parse(tokens("p.a > 1 OR p.b > 2 AND p.c > 3")));

        var or = assertInstanceOf(OrExpression.class, ok.token());
        assertInstanceOf(ComparisonExpression.class, or.left());
        assertInstanceOf(AndExpression.class, or.right());
    }

    @Test
    void parenthesesOverrideThePrecedence() {
        // los mismos operadores, pero ahora el OR queda adentro
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.expression().parse(tokens("(p.a > 1 OR p.b > 2) AND p.c > 3")));

        var and = assertInstanceOf(AndExpression.class, ok.token());
        assertInstanceOf(OrExpression.class, and.left());
        assertInstanceOf(ComparisonExpression.class, and.right());
    }

    @Test
    void orAssociatesToTheRight() {
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.expression().parse(tokens("p.a > 1 OR p.b > 2 OR p.c > 3")));

        assertInstanceOf(OrExpression.class, ((OrExpression) ok.token()).right());
    }

    @Test
    void aGroupDoesNotWrapAnything() {
        // los parentesis no son un nodo del AST: solo agrupan
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.expression().parse(tokens("((p.a > 1))")));

        assertInstanceOf(ComparisonExpression.class, ok.token());
    }

    @Test
    void notCanNegateAWholeGroup() {
        var ok = assertInstanceOf(Ok.class,
                ExpressionParsers.expression().parse(tokens("NOT (p.a > 1 OR p.b > 2)")));

        var not = assertInstanceOf(NotExpression.class, ok.token());
        assertInstanceOf(OrExpression.class, not.operand());
    }

    @Test
    void aGroupWithoutClosingFails() {
        assertInstanceOf(Fail.class, ExpressionParsers.expression().parse(tokens("(p.a > 1")));
    }
}
