package dev.rodrigosambade.chat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class ChatServer implements AutoCloseable {

    private static final String NICK_COMMAND = "NICK ";
    private static final String MESSAGE_COMMAND = "MSG ";
    private static final int DEFAULT_PORT = 5_000;

    private final ServerSocket serverSocket;
    private final ExecutorService workers = Executors.newCachedThreadPool();
    private final NickRegistry<ClientSession> registry = new NickRegistry<>();

    private volatile boolean running = true;

    public ChatServer(int port) throws IOException {
        serverSocket = new ServerSocket(port);
    }

    public int port() {
        return serverSocket.getLocalPort();
    }

    public void serve() throws IOException {
        while (running) {
            try {
                Socket socket = serverSocket.accept();
                workers.submit(() -> handle(socket));
            } catch (SocketException exception) {
                if (running) {
                    throw exception;
                }
            }
        }
    }

    private void handle(Socket socket) {
        ClientSession session = null;
        String nickname = null;

        try (socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(
                     socket.getInputStream(),
                     StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                     socket.getOutputStream(),
                     StandardCharsets.UTF_8))) {
            String hello = input.readLine();
            nickname = parseNickname(hello, output);
            if (nickname == null) {
                return;
            }

            session = new ClientSession(output);
            if (!registry.register(nickname, session)) {
                send(output, "ERROR duplicate nick");
                return;
            }

            send(output, "OK");
            broadcast("JOIN " + nickname);
            broadcastUsers();
            processMessages(nickname, input, output);
        } catch (IOException ignored) {
            // A failed client connection must not stop the chat server.
        } finally {
            unregister(nickname, session);
        }
    }

    private static String parseNickname(String hello, BufferedWriter output)
            throws IOException {
        if (hello == null || !hello.startsWith(NICK_COMMAND)) {
            send(output, "ERROR expected NICK");
            return null;
        }

        String nickname = hello.substring(NICK_COMMAND.length()).trim();
        if (nickname.isEmpty()) {
            send(output, "ERROR empty nick");
            return null;
        }
        return nickname;
    }

    private void processMessages(
            String nickname,
            BufferedReader input,
            BufferedWriter output) throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            if (line.equals("QUIT")) {
                return;
            }

            if (line.startsWith(MESSAGE_COMMAND)) {
                String message = line.substring(MESSAGE_COMMAND.length());
                broadcast("MESSAGE " + nickname + " " + message);
            } else {
                send(output, "ERROR expected MSG or QUIT");
            }
        }
    }

    private void unregister(String nickname, ClientSession session) {
        if (nickname == null || session == null) {
            return;
        }

        registry.remove(nickname, session);
        broadcast("LEAVE " + nickname);
        broadcastUsers();
    }

    private void broadcastUsers() {
        broadcast("USERS " + String.join(",", registry.nicks()));
    }

    private void broadcast(String message) {
        for (ClientSession client : registry.values()) {
            client.send(message);
        }
    }

    private static void send(BufferedWriter output, String message) throws IOException {
        output.write(message);
        output.newLine();
        output.flush();
    }

    @Override
    public void close() throws IOException {
        running = false;
        serverSocket.close();
        workers.shutdownNow();
    }

    public static void main(String[] args) throws IOException {
        int port = args.length > 0
                ? Integer.parseInt(args[0])
                : DEFAULT_PORT;

        try (ChatServer server = new ChatServer(port)) {
            System.out.println("Chat en puerto " + server.port());
            server.serve();
        }
    }

    private static final class ClientSession {

        private final BufferedWriter output;

        private ClientSession(BufferedWriter output) {
            this.output = output;
        }

        private synchronized void send(String message) {
            try {
                ChatServer.send(output, message);
            } catch (IOException ignored) {
                // The connection cleanup path will remove the failed client.
            }
        }
    }
}
