package com.cyphail.engine;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompilerEngineBrokerTest {

    @Test
    void validQueryIsCompiledAndSentToEngine() {
        AtomicReference<String> receivedSource =
                new AtomicReference<>();

        AtomicReference<String> receivedCode =
                new AtomicReference<>();

        EngineBroker fakeEngine = new EngineBroker() {
            @Override
            public EngineResult execute(String statement) {
                return EngineResult.error(
                        "The compiled overload was not used."
                );
            }

            @Override
            public EngineResult execute(
                    String originalStatement,
                    String generatedCode
            ) {
                receivedSource.set(originalStatement);
                receivedCode.set(generatedCode);
                return EngineResult.ok("Executed.");
            }
        };

        CompilerEngineBroker broker =
                new CompilerEngineBroker(fakeEngine);

        EngineResult result = broker.execute(
                "MATCH (m:Movie) RETURN m.title AS title"
        );

        assertTrue(result.success());
        assertEquals("Executed.", result.output());

        assertEquals(
                "MATCH (m:Movie) RETURN m.title AS title",
                receivedSource.get()
        );

        assertTrue(
                receivedCode.get().startsWith("query([match(")
        );
    }

    @Test
    void undefinedVariableDoesNotReachEngine() {
        AtomicReference<String> received =
                new AtomicReference<>();

        EngineBroker fakeEngine = statement -> {
            received.set(statement);
            return EngineResult.ok("Should not execute.");
        };

        CompilerEngineBroker broker =
                new CompilerEngineBroker(fakeEngine);

        EngineResult result = broker.execute(
                "MATCH (p:Person) "
                        + "WHERE q.age > 60 "
                        + "RETURN q AS name"
        );

        assertFalse(result.success());
        assertEquals(
                "Undefined variable 'q'",
                result.output()
        );
        assertNull(received.get());
    }

    @Test
    void syntaxErrorDoesNotReachEngine() {
        AtomicReference<String> received =
                new AtomicReference<>();

        EngineBroker fakeEngine = statement -> {
            received.set(statement);
            return EngineResult.ok("Should not execute.");
        };

        CompilerEngineBroker broker =
                new CompilerEngineBroker(fakeEngine);

        EngineResult result = broker.execute(
                "MATCH (p RETURN p"
        );

        assertFalse(result.success());
        assertTrue(
                result.output().startsWith("Syntax error:")
        );
        assertNull(received.get());
    }
}