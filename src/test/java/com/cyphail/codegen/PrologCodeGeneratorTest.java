/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Code Generator Tests)
 */
package com.cyphail.codegen;

import com.cyphail.ast.AndExpression;
import com.cyphail.ast.ComparisonExpression;
import com.cyphail.ast.ComparisonOperator;
import com.cyphail.ast.NumberLiteral;
import com.cyphail.ast.DecimalLiteral;
import com.cyphail.ast.Direction;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.PatternStep;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.RelationshipPattern;
import com.cyphail.ast.RemoveStatement;
import com.cyphail.ast.ReturnItem;
import com.cyphail.ast.SetItem;
import com.cyphail.ast.SetStatement;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PrologCodeGeneratorTest {

    @Test
    void generatesPrologForSimpleMatchQuery() {
        NodePattern movieNode = new NodePattern(
                Optional.of("m"),
                List.of("Movie"),
                List.of()
        );

        MatchStatement match = new MatchStatement(
                List.of(
                        new PathPattern(
                                movieNode,
                                List.of()
                        )
                ),
                Optional.empty()
        );

        ReturnItem title = new ReturnItem(
                new PropertyAccessExpression(
                        "m",
                        "title"
                ),
                Optional.of("title")
        );

        Program program = new Program(
                List.of(match),
                List.of(title)
        );

        String result =
                PrologCodeGenerator.generate(program);

        String expected =
                "query([match([node(var('m'), ['Movie'], [])], none)], "
                        + "[return_item(property_access('m', 'title'), 'title')]).";

        assertEquals(expected, result);
    }

    @Test
    void generatesPrologForRelationshipPath() {
        NodePattern person = new NodePattern(
                Optional.of("p"),
                List.of("Person"),
                List.of()
        );

        NodePattern movie = new NodePattern(
                Optional.of("m"),
                List.of("Movie"),
                List.of()
        );

        RelationshipPattern actedIn =
                new RelationshipPattern(
                        Optional.of("r"),
                        Optional.of("ACTED_IN"),
                        List.of(),
                        Direction.OUT
                );

        PatternStep step = new PatternStep(
                actedIn,
                movie
        );

        PathPattern path = new PathPattern(
                person,
                List.of(step)
        );

        MatchStatement match = new MatchStatement(
                List.of(path),
                Optional.empty()
        );

        ReturnItem title = new ReturnItem(
                new PropertyAccessExpression(
                        "m",
                        "title"
                ),
                Optional.of("title")
        );

        Program program = new Program(
                List.of(match),
                List.of(title)
        );

        String result =
                PrologCodeGenerator.generate(program);

        String expected =
                "query([match([path("
                        + "node(var('p'), ['Person'], []), "
                        + "[step(relationship("
                        + "var('r'), 'ACTED_IN', [], out), "
                        + "node(var('m'), ['Movie'], []))]"
                        + ")], none)], "
                        + "[return_item(property_access("
                        + "'m', 'title'), 'title')]).";

        assertEquals(expected, result);
    }

    @Test
    void generatesPrologForSetStatement() {
        SetItem price = new SetItem(
                new PropertyAccessExpression(
                        "p",
                        "precio"
                ),
                new DecimalLiteral(19.99)
        );

        SetStatement set = new SetStatement(
                List.of(price)
        );

        Program program = new Program(
                List.of(set),
                List.of()
        );

        String result =
                PrologCodeGenerator.generate(program);

        String expected =
                "query([set([set_item("
                        + "property_access('p', 'precio'), "
                        + "19.99)])], []).";

        assertEquals(expected, result);
    }

    @Test
    void generatesPrologForRemoveStatement() {
        RemoveStatement remove =
                new RemoveStatement(
                        List.of(
                                new PropertyAccessExpression(
                                        "p",
                                        "discount"
                                )
                        )
                );

        Program program = new Program(
                List.of(remove),
                List.of()
        );

        String result =
                PrologCodeGenerator.generate(program);

        String expected =
                "query([remove([property_access("
                        + "'p', 'discount')])], []).";

        assertEquals(expected, result);
    }

    @Test
    void generatesPrologForAndExpression() {
        NodePattern person = new NodePattern(
                Optional.of("p"),
                List.of("Person"),
                List.of()
        );

        ComparisonExpression olderThan18 =
                new ComparisonExpression(
                        new PropertyAccessExpression(
                                "p",
                                "age"
                        ),
                        ComparisonOperator.GREATER_THAN,
                        new NumberLiteral(18)
                );

        ComparisonExpression youngerThan65 =
                new ComparisonExpression(
                        new PropertyAccessExpression(
                                "p",
                                "age"
                        ),
                        ComparisonOperator.LESS_THAN,
                        new NumberLiteral(65)
                );

        AndExpression condition = new AndExpression(
                olderThan18,
                youngerThan65
        );

        MatchStatement match = new MatchStatement(
                List.of(
                        new PathPattern(
                                person,
                                List.of()
                        )
                ),
                Optional.of(condition)
        );

        Program program = new Program(
                List.of(match),
                List.of()
        );

        String result =
                PrologCodeGenerator.generate(program);

        String expected =
                "query([match([node(var('p'), ['Person'], [])], "
                        + "and(compare(property_access('p', 'age'), '>', 18), "
                        + "compare(property_access('p', 'age'), '<', 65)))], []).";

        assertEquals(expected, result);
    }


}