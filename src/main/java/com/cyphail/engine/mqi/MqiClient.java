/*
 Cyphail - Grupo 4 (1pm) - EIF400-II-2026-CLoria
 Autor: Jose Moya Perez (SWI-Prolog MQI Client)
 */
package com.cyphail.engine.mqi;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Objects;

public final class MqiClient implements AutoCloseable {

    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private final int queryTimeoutSeconds;

    public MqiClient(
            String host,
            int port,
            String password,
            Duration timeout
    ) throws IOException {
        Objects.requireNonNull(host, "host cannot be null");
        Objects.requireNonNull(password, "password cannot be null");
        Objects.requireNonNull(timeout, "timeout cannot be null");

        int timeoutMillis = Math.toIntExact(timeout.toMillis());

        socket = new Socket();
        socket.connect(
                new InetSocketAddress(host, port),
                timeoutMillis
        );
        socket.setSoTimeout(timeoutMillis);

        input = socket.getInputStream();
        output = socket.getOutputStream();
        queryTimeoutSeconds = Math.toIntExact(timeout.toSeconds());

        sendMessage(password);

        JsonElement handshake = receiveJson();

        if (!isSuccessfulHandshake(handshake)) {
            close();
            throw new IOException(
                    "SWI-Prolog MQI rejected the handshake: "
                            + handshake
            );
        }
    }

    public JsonElement query(String goal) throws IOException {
        Objects.requireNonNull(goal, "goal cannot be null");

        if (goal.isBlank()) {
            throw new IllegalArgumentException(
                    "goal cannot be blank"
            );
        }

        String normalized = removeFinalPeriod(goal.strip());

        sendMessage(
                "run(("
                        + normalized
                        + "), "
                        + queryTimeoutSeconds
                        + ")"
        );

        return receiveJson();
    }

    private void sendMessage(String value) throws IOException {
        String normalized = removeFinalPeriod(value.strip());
        byte[] payload = (normalized + ".\n")
                .getBytes(StandardCharsets.UTF_8);

        byte[] header = (payload.length + ".\n")
                .getBytes(StandardCharsets.UTF_8);

        output.write(header);
        output.write(payload);
        output.flush();
    }

    private JsonElement receiveJson() throws IOException {
        String payload = receiveMessage().strip();

        // Algunas versiones del protocolo pueden dejar el punto final.
        if (payload.endsWith(".")) {
            payload = payload.substring(
                    0,
                    payload.length() - 1
            ).stripTrailing();
        }

        try {
            return JsonParser.parseString(payload);
        } catch (RuntimeException exception) {
            throw new IOException(
                    "Invalid JSON received from SWI-Prolog MQI: "
                            + payload,
                    exception
            );
        }
    }

    private String receiveMessage() throws IOException {
        String header;

        do {
            header = readProtocolLine().strip();
        } while (header.isEmpty() || header.equals("."));

        if (!header.endsWith(".")) {
            throw new IOException(
                    "Invalid MQI frame header: " + header
            );
        }

        String lengthText = header.substring(
                0,
                header.length() - 1
        );

        final int length;

        try {
            length = Integer.parseInt(lengthText);
        } catch (NumberFormatException exception) {
            throw new IOException(
                    "Invalid MQI frame length: " + header,
                    exception
            );
        }

        byte[] payload = input.readNBytes(length);

        if (payload.length != length) {
            throw new IOException(
                    "Incomplete MQI frame. Expected "
                            + length
                            + " bytes but received "
                            + payload.length
            );
        }

        return new String(payload, StandardCharsets.UTF_8);
    }

    private String readProtocolLine() throws IOException {
        ByteArrayOutputStream line = new ByteArrayOutputStream();

        while (true) {
            int value = input.read();

            if (value == -1) {
                throw new IOException(
                        "SWI-Prolog MQI closed the connection"
                );
            }

            if (value == '\n') {
                return line.toString(StandardCharsets.UTF_8);
            }

            // Permite respuestas con \r\n en Windows.
            if (value != '\r') {
                line.write(value);
            }
        }
    }

    private static boolean isSuccessfulHandshake(
            JsonElement response
    ) {
        if (!response.isJsonObject()) {
            return false;
        }

        JsonObject object = response.getAsJsonObject();

        return object.has("functor")
                && object.get("functor").getAsString()
                .equals("true");
    }

    private static String removeFinalPeriod(String value) {
        return value.endsWith(".")
                ? value.substring(0, value.length() - 1)
                : value;
    }

    @Override
    public void close() throws IOException {
        try {
            if (!socket.isClosed()) {
                sendMessage("close");
            }
        } finally {
            socket.close();
        }
    }
}
