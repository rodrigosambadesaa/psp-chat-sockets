package dev.rodrigosambade.chat;

import dev.rodrigosambade.net.ConnectivityAndInternetAccess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class ChatClient implements AutoCloseable {

    private static final Duration CONNECTIVITY_TIMEOUT = Duration.ofSeconds(2);
    private static final int CONNECT_TIMEOUT_MS = 3_000;

    private final Socket socket;
    private final BufferedReader input;
    private final BufferedWriter output;

    public ChatClient(String host, int port, String nickname) throws IOException {
        ConnectivityAndInternetAccess.requireInternetForRemoteHost(
                host,
                CONNECTIVITY_TIMEOUT);

        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        input = new BufferedReader(new InputStreamReader(
                socket.getInputStream(),
                StandardCharsets.UTF_8));
        output = new BufferedWriter(new OutputStreamWriter(
                socket.getOutputStream(),
                StandardCharsets.UTF_8));

        authenticate(nickname);
    }

    public void sendMessage(String message) throws IOException {
        if (message.contains("\n") || message.contains("\r")) {
            throw new IllegalArgumentException("Messages must fit on one line");
        }
        sendRaw("MSG " + message);
    }

    public String readEvent() throws IOException {
        return input.readLine();
    }

    private void authenticate(String nickname) throws IOException {
        sendRaw("NICK " + nickname);
        String answer = input.readLine();

        if (!"OK".equals(answer)) {
            socket.close();
            throw new IOException(answer == null ? "Server closed connection" : answer);
        }
    }

    private void sendRaw(String line) throws IOException {
        output.write(line);
        output.newLine();
        output.flush();
    }

    @Override
    public void close() throws IOException {
        try {
            if (!socket.isClosed()) {
                sendRaw("QUIT");
            }
        } catch (IOException ignored) {
            // The socket may already have been closed by the server.
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            throw new IllegalArgumentException("Usage: host port nick");
        }

        try (ChatClient client = new ChatClient(
                args[0],
                Integer.parseInt(args[1]),
                args[2]);
             BufferedReader console = new BufferedReader(new InputStreamReader(
                     System.in,
                     StandardCharsets.UTF_8))) {
            Thread readerThread = startEventReader(client);
            readConsole(client, console);
            readerThread.interrupt();
        }
    }

    private static Thread startEventReader(ChatClient client) {
        return Thread.ofPlatform().start(() -> {
            try {
                String event;
                while ((event = client.readEvent()) != null) {
                    System.out.println(event);
                }
            } catch (IOException ignored) {
                // Closing the client ends this reader loop.
            }
        });
    }

    private static void readConsole(ChatClient client, BufferedReader console)
            throws IOException {
        String line;
        while ((line = console.readLine()) != null
                && !line.equalsIgnoreCase("/quit")) {
            client.sendMessage(line);
        }
    }
}
