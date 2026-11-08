/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (casos de P2)
 */
package com.cyphail.casos;

import com.cyphail.analyzer.Analyzer;
import com.cyphail.ast.Program;
import com.cyphail.cli.ReplInput;
import com.cyphail.cli.StatementSplitter;
import com.cyphail.codegen.PrologCodeGenerator;
import com.cyphail.lexer.Ok;
import com.cyphail.parser.CyphailParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

// Revisa los archivos de casos-p2/ sin levantar SWI-Prolog. Por eso corre
// siempre, tambien en una maquina sin el motor instalado: una prueba que se
// omite no demuestra nada.
class CasosP2Test {

    private static final Path FOLDER = Path.of("casos-p2");

    // Los archivos se descubren, no se listan: un caso nuevo entra solo.
    // Si la carpeta no existe, el metodo revienta y la prueba falla, que es
    // lo que se quiere -- no que pase en silencio sin revisar nada.
    static List<Path> archivos() throws IOException {
        try (var files = Files.list(FOLDER)) {
            return files.filter(path -> path.toString().endsWith(".cyphail"))
                        .sorted()
                        .toList();
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("archivos")
    void cadaEstatutoParseaYPasaElAnalizador(Path archivo) throws IOException {
        var statements = estatutosDe(archivo, Files.readString(archivo));

        assertFalse(statements.isEmpty(), archivo + ": no trae ningun estatuto");

        statements.forEach(statement -> revisar(archivo, statement));
    }

    // Las DOS cosas, y en este orden. Parsear no alcanza: cuando el analizador
    // todavia no registraba las variables de relacion,
    // "MATCH (a)-[r]->(b) RETURN r" parseaba perfecto y estaba mal. Una prueba
    // de sola sintaxis habria estado verde todo ese tiempo.
    //
    // La tercera es gratis y cubre un error real que ya tuvimos: el codegen
    // se tragaba las relaciones en silencio y respondia OK.
    private static void revisar(Path archivo, String statement) {
        var parsed = assertInstanceOf(Ok.class, CyphailParser.parse(statement),
                () -> fallo(archivo, statement, "no parseo"));

        var program = (Program) parsed.token();

        assertEquals(Optional.empty(), Analyzer.analyze(program),
                () -> fallo(archivo, statement, "el analizador lo rechazo"));

        assertDoesNotThrow(() -> PrologCodeGenerator.generate(program),
                () -> fallo(archivo, statement, "el codegen fallo"));
    }

    // Se recortan igual que en el REPL y en .load: sin lineas de comentario y
    // cortando en ';'. Asi la prueba lee los archivos como el usuario.
    private static List<String> estatutosDe(Path archivo, String source) {
        var sinComentarios = source.lines()
                .filter(line -> !ReplInput.isComment(line))
                .collect(Collectors.joining("\n"));

        var split = StatementSplitter.split(sinComentarios);

        assertTrue(split.rest().isBlank(),
                () -> fallo(archivo, split.rest(), "quedo sin cerrar con ';'"));

        return split.statements();
    }

    @Test
    void laCarpetaTraeElGrafoBase() {
        // sin el grafo los casos corren pero no devuelven filas: es el primer
        // paso de la demo y el README de la carpeta lo pone de primero
        assertTrue(Files.exists(FOLDER.resolve("grafo.cyphail")),
                "falta casos-p2/grafo.cyphail");
    }

    // Un estatuto puede ocupar varias lineas; para el mensaje basta la primera.
    private static String fallo(Path archivo, String statement, String que) {
        var primera = statement.strip().lines().findFirst().orElse("(vacio)");
        var corta = primera.length() <= 60 ? primera : primera.substring(0, 60) + "...";

        return "%s: %s -> \"%s\"".formatted(archivo, que, corta);
    }
}
