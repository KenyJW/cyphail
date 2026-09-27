/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor original: Kenny Jimenez Wang (CLI/Compiler)
 Modificado por: Jose Moya Perez (Prolog Code Generator)
 */
package com.cyphail.cli;

import com.cyphail.analyzer.Analyzer;
import com.cyphail.ast.Program;
import com.cyphail.codegen.PrologCodeGenerator;
import com.cyphail.lexer.Fail;
import com.cyphail.lexer.Ok;
import com.cyphail.parser.CyphailParser;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;

@Command(
        name = "compile",
        description = "Compiles a Cyphail script to Prolog."
)
public final class CompileCommand implements Callable<Integer> {

    @Parameters(
            index = "0",
            description = "Path to the input .cyphail script."
    )
    private Path source;

    @Option(
            names = "--out",
            description = "Output path for the generated .pl file."
    )
    private Path outPath;

    private final PrintStream out;

    public CompileCommand() {
        this(System.out);
    }

    CompileCommand(PrintStream out) {
        this.out = out;
    }

    @Override
    public Integer call() {
        if (source == null || !Files.exists(source)) {
            out.println("ERROR: file not found: " + source);
            return 1;
        }

        try {
            String cyphailSource = Files.readString(
                    source,
                    StandardCharsets.UTF_8
            );

            return switch (CyphailParser.parse(cyphailSource)) {
                case Fail<?, ?, ?> fail -> {
                    out.println(
                            "ERROR: Syntax error: " + fail.reason()
                    );
                    yield 1;
                }

                case Ok<?, ?, ?> ok -> {
                    Program program = (Program) ok.token();
                    var analyzerError = Analyzer.analyze(program);

                    if (analyzerError.isPresent()) {
                        out.println(
                                "ERROR: " + analyzerError.get()
                        );
                        yield 1;
                    }

                    String prologCode =
                            PrologCodeGenerator.generate(program);

                    yield writeResult(prologCode);
                }
            };
        } catch (IOException exception) {
            out.println(
                    "ERROR: could not read file: "
                            + exception.getMessage()
            );
            return 1;
        }
    }

    private Integer writeResult(String prologCode) {
        if (outPath == null) {
            out.println(prologCode);
            return 0;
        }

        try {
            Path parent = outPath.getParent();

            if (parent != null) {
                Files.createDirectories(parent);
            }

            Files.writeString(
                    outPath,
                    prologCode + System.lineSeparator(),
                    StandardCharsets.UTF_8
            );

            out.println(
                    "OK. Prolog code written to " + outPath
            );

            return 0;
        } catch (IOException exception) {
            out.println(
                    "ERROR: could not write file: "
                            + exception.getMessage()
            );
            return 1;
        }
    }
}