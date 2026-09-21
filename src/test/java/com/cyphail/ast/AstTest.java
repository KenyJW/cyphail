/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (AST)
 */
package com.cyphail.ast;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstTest{
	@Test
	void caso1SeRepresentaComoAst(){
		Program program = new Program(
        List.of(new MatchStatement(List.of(new NodePattern(Optional.of("m"), List.of("Movie"), List.of())),Optional.empty())),
        List.of(new ReturnItem(new PropertyAccessExpression("m", "title"), Optional.empty()),
                new ReturnItem(new PropertyAccessExpression("m", "year"), Optional.of("year"))));
		assertEquals(1, program.clauses().size());
		assertEquals(2, program.returnItems().size());
	}
}