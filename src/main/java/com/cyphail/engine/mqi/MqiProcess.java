/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog MQI Process)
 */
package com.cyphail.engine.mqi;

import java.io.BufferedReader;
import java.io.IOException;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class MqiProcess implements AutoCloseable {

    private final Process process;
    private final int port;
    private final String password;

    private MqiProcess(
            Process process,
            int port,
            String password
    ) {
        this.process = process;
        this.port = port;
        this.password = password;
    }

    public static MqiProcess start(
            String executable,
            Duration startupTimeout
    ) throws IOException {
        Objects.requireNonNull(
                executable,
                "executable cannot be null"
        );
        Objects.requireNonNull(
                startupTimeout,
                "startupTimeout cannot be null"
        );

        Process process = new ProcessBuilder(
                executable,
                "mqi",
                "--write_connection_values=true"
        ).start();

        try {
            ConnectionValues values = readConnectionValues(
                    process,
                    startupTimeout
            );

            return new MqiProcess(
                    process,
                    values.port(),
                    values.password()
            );
        } catch (IOException exception) {
            process.destroyForcibly();
            throw exception;
        }
    }

    public MqiClient openClient(
            Duration timeout
    ) throws IOException {
        if (!process.isAlive()) {
            throw new IOException(
                    "SWI-Prolog MQI process is not running"
            );
        }

        return new MqiClient(
                "127.0.0.1",
                port,
                password,
                timeout
        );
    }

    public int port() {
        return port;
    }

    private static ConnectionValues readConnectionValues(
            Process process,
            Duration timeout
    ) throws IOException {
        ExecutorService executor =
                Executors.newSingleThreadExecutor();

        try {
            Future<ConnectionValues> result = executor.submit(
                    () -> {
                        BufferedReader output =
                                process.inputReader();

                        String portText = output.readLine();
                        String password = output.readLine();

                        if (portText == null
                                || password == null) {
                            throw new IOException(
                                    "SWI-Prolog did not provide "
                                            + "MQI connection values"
                            );
                        }

                        try {
                            return new ConnectionValues(
                                    Integer.parseInt(
                                            portText.strip()
                                    ),
                                    password.strip()
                            );
                        } catch (NumberFormatException exception) {
                            throw new IOException(
                                    "Invalid MQI port: "
                                            + portText,
                                    exception
                            );
                        }
                    }
            );

            try {
                return result.get(
                        timeout.toMillis(),
                        TimeUnit.MILLISECONDS
                );
            } catch (TimeoutException exception) {
                result.cancel(true);
                throw new IOException(
                        "Timed out while starting SWI-Prolog MQI",
                        exception
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException(
                        "Interrupted while starting SWI-Prolog MQI",
                        exception
                );
            } catch (ExecutionException exception) {
                Throwable cause = exception.getCause();

                if (cause instanceof IOException ioException) {
                    throw ioException;
                }

                throw new IOException(
                        "Could not start SWI-Prolog MQI",
                        cause
                );
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Override
    public void close() {
        process.destroy();

        try {
            if (!process.waitFor(2, TimeUnit.SECONDS)) {
                process.destroyForcibly();
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
        }
    }

    private record ConnectionValues(
            int port,
            String password
    ) {
    }
}