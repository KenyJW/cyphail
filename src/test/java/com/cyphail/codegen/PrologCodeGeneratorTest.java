/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Code Generator Tests)
 */
package com.cyphail.codegen;

import com.cyphail.ast.Direction;
import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.PathPattern;
import com.cyphail.ast.PatternStep;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
import com.cyphail.ast.RelationshipPattern;
import com.cyphail.ast.ReturnItem;
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
}