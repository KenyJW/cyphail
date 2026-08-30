/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Kenny Jimenez Wang (CLI/REPL)
 */
package com.cyphail.cli;

import com.cyphail.frontend.CyphailResponse;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplCommandTest {

    @Test
    void printsWelcomeBannerAndPromptOnStart() throws Exception {
        var result = run("");

        assertTrue(result.output().contains("Welcome to Cyphail-"));
        assertTrue(result.output().contains(">>> "));
        assertTrue(result.output().contains("Bye!"));
    }

    @Test
    void dispatchesNonDotLinesUntilExit() throws Exception {
        String script = "MATCH (p:Person) RETURN p.name\n.exit\nesto no deberia llegar\n";
        List<String> received = new ArrayList<>();

        var result = run(script, input -> {
            received.add(input);
            return CyphailResponse.ok("ok: " + input);
        });

        assertEquals(0, result.exitCode());
        assertEquals(List.of("MATCH (p:Person) RETURN p.name"), received);
    }

    @Test
    void skipsBlankLinesWithoutCallingHandler() throws Exception {
        String script = "\n\n.exit\n";
        List<String> received = new ArrayList<>();

        run(script, input -> {
            received.add(input);
            return CyphailResponse.ok("ok");
        });

        assertTrue(received.isEmpty());
    }

    @Test
    void dotHelpShowsCommandList() throws Exception {
        var result = run(".help\n.exit\n");

        assertTrue(result.output().contains(".about"));
        assertTrue(result.output().contains(".use"));
    }

    @Test
    void dotAboutShowsAuthors() throws Exception {
        var result = run(".about\n.exit\n");

        assertTrue(result.output().contains("Kenny"));
        assertTrue(result.output().contains("Moya"));
        assertTrue(result.output().contains("Sebastian"));
    }

    @Test
    void dotUseWithoutArgumentListsGraphs() throws Exception {
        var result = run(".use\n.exit\n");

        assertTrue(result.output().contains("dragon"));
        assertTrue(result.output().contains("Description"));
    }

    @Test
    void dotUseWithKnownGraphConnects() throws Exception {
        var result = run(".use dragon\n.exit\n");

        assertTrue(result.output().contains("OK. \"dragon\" graph available after"));
    }

    @Test
    void dotUseWithUnknownGraphReportsError() throws Exception {
        var result = run(".use nope\n.exit\n");

        assertTrue(result.output().contains("ERROR"));
        assertTrue(result.output().contains("nope"));
    }

    @Test
    void unknownDotCommandReportsError() throws Exception {
        var result = run(".bogus\n.exit\n");

        assertTrue(result.output().contains("ERROR"));
    }

    @Test
    void failedResponseIsPrefixedWithError() throws Exception {
        var result = run("MATCH broken\n.exit\n", input -> CyphailResponse.error("bad statement"));

        assertTrue(result.output().contains("ERROR: bad statement"));
    }

    private record ReplRun(String output, int exitCode) {
    }

    private ReplRun run(String script) throws Exception {
        return run(script, input -> CyphailResponse.ok("ok: " + input));
    }

    private ReplRun run(String script, com.cyphail.frontend.RequestHandler handler) throws Exception {
        var in = new ByteArrayInputStream(script.getBytes(StandardCharsets.UTF_8));
        var outBuffer = new ByteArrayOutputStream();
        var out = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);

        ReplCommand repl = new ReplCommand(handler, in, out);
        int exitCode = repl.call();

        return new ReplRun(outBuffer.toString(StandardCharsets.UTF_8), exitCode);
    }
}
