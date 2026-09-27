/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (Compiler / Engine Broker)
 */
package com.cyphail.engine;

import com.cyphail.analyzer.Analyzer;
import com.cyphail.ast.Program;
import com.cyphail.codegen.PrologCodeGenerator;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Ok;
import com.cyphail.parser.CyphailParser;

import java.util.Objects;

public final class CompilerEngineBroker implements EngineBroker {

    private final EngineBroker engine;

    public CompilerEngineBroker(EngineBroker engine) {
        this.engine = Objects.requireNonNull(
                engine,
                "engine cannot be null"
        );
    }

    @Override
    public EngineResult execute(String statement) {
        if (statement == null || statement.isBlank()) {
            return EngineResult.error("Empty statement.");
        }

        String source = statement.strip();

        return switch (CyphailParser.parse(source)) {
            case Fail<?, ?, ?> fail ->
                    EngineResult.error(
                            "Syntax error: " + fail.reason()
                    );

            case Ok<?, ?, ?> ok -> {
                Program program = (Program) ok.token();

                var analyzerError = Analyzer.analyze(program);

                if (analyzerError.isPresent()) {
                    yield EngineResult.error(
                            analyzerError.get()
                    );
                }

                String generatedCode =
                        PrologCodeGenerator.generate(program);

                yield engine.execute(source, generatedCode);
            }
        };
    }
}
