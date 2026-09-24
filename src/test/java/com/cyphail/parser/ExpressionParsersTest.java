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
}
