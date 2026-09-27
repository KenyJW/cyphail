package com.cyphail.codegen;

import com.cyphail.ast.MatchStatement;
import com.cyphail.ast.NodePattern;
import com.cyphail.ast.Program;
import com.cyphail.ast.PropertyAccessExpression;
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
                List.of(movieNode),
                Optional.empty()
        );

        ReturnItem title = new ReturnItem(
                new PropertyAccessExpression("m", "title"),
                Optional.of("title")
        );

        Program program = new Program(
                List.of(match),
                List.of(title)
        );

        String result = PrologCodeGenerator.generate(program);

        String expected =
                "query([match([node(var('m'), ['Movie'], [])], none)], "
                        + "[return_item(property_access('m', 'title'), 'title')]).";

        assertEquals(expected, result);
    }
}