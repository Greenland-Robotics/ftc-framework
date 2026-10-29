package visualizer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A tiny HTTP server: just enough to serve the visualizer's page and its JSON. It is built on
 * plain sockets (instead of the JDK's com.sun.net.httpserver) so it compiles against the same
 * libraries as the robot code, which is how VS Code checks this folder.
 *
 * Connections stay open for more requests, and a response can be a stream of server-sent events.
 * Both matter through VS Code's port forwarding from a dev container, where every new connection
 * costs about a third of a second.
 */
final class WebServer {
    static final class Request {
        final String method;
        final String path;
        final Map<String, String> query;

        Request(String method, String path, Map<String, String> query) {
            this.method = method;
            this.path = path;
            this.query = query;
        }
    }

    static class Response {
        final int code;
        final String type;
        final byte[] body;

        Response(int code, String type, byte[] body) {
            this.code = code;
            this.type = type;
            this.body = body;
        }

        static Response text(int code, String text) {
            return new Response(code, "text/plain; charset=utf-8", text.getBytes(StandardCharsets.UTF_8));
        }
    }

    /** A response that keeps sending server-sent events until the page goes away. */
    static final class Stream extends Response {
        final Events events;

        Stream(Events events) {
            super(200, "text/event-stream", null);
            this.events = events;
        }
    }

    interface Events {
        /** Writes events with {@link #send} until it throws (the page went away). */
        void run(OutputStream out) throws IOException, InterruptedException;
    }

    /** Sends one server-sent event. */
    static void send(OutputStream out, String event, String data) throws IOException {
        out.write(("event: " + event + "\ndata: " + data + "\n\n").getBytes(StandardCharsets.UTF_8));
        out.flush();
    }

    interface Handler {
        Response handle(Request request) throws IOException;
    }

    private final ServerSocket socket;
    private final Handler handler;

    /** Listens on {@code address} (null for every interface) and {@code port}. */
    WebServer(InetAddress address, int port, Handler handler) throws IOException {
        this.handler = handler;
        socket = new ServerSocket();
        socket.setReuseAddress(true);
        socket.bind(new InetSocketAddress(address, port), 50);
    }

    void start() {
        Thread accept = new Thread(() -> {
            while (!socket.isClosed()) {
                try {
                    Socket client = socket.accept();
                    Thread serve = new Thread(() -> serve(client), "visualizer-request");
                    serve.setDaemon(true);
                    serve.start();
                } catch (IOException e) {
                    // Keep accepting
                }
            }
        }, "visualizer-web");
        accept.setDaemon(true);
        accept.start();
    }

    private void serve(Socket client) {
        try (Socket connection = client) {
            connection.setSoTimeout(30_000);   // Close a connection that sits idle this long
            connection.setTcpNoDelay(true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.ISO_8859_1));
            OutputStream out = connection.getOutputStream();
            // One request after another on the same connection, until either side closes it
            while (true) {
                String requestLine = in.readLine();
                if (requestLine == null) return;
                if (requestLine.isEmpty()) continue;
                long bodyLength = 0;
                boolean close = false;
                for (String header = in.readLine(); header != null && !header.isEmpty(); header = in.readLine()) {
                    String lower = header.toLowerCase(java.util.Locale.ROOT);
                    if (lower.startsWith("content-length:")) {
                        bodyLength = Long.parseLong(lower.substring(15).trim());
                    } else if (lower.startsWith("connection:") && lower.contains("close")) {
                        close = true;
                    }
                }
                // Requests carry everything in the URL; skip any body
                for (long skipped = 0; skipped < bodyLength; skipped++) {
                    if (in.read() < 0) return;
                }

                String[] parts = requestLine.split(" ");
                Response response;
                if (parts.length < 2) {
                    response = Response.text(400, "Bad request");
                    close = true;
                } else {
                    String target = parts[1];
                    int q = target.indexOf('?');
                    Request request = new Request(parts[0], q < 0 ? target : target.substring(0, q),
                            query(q < 0 ? null : target.substring(q + 1)));
                    try {
                        response = handler.handle(request);
                    } catch (IOException | RuntimeException e) {
                        response = Response.text(500, e.toString());
                    }
                }
                if (response instanceof Stream) {
                    stream(connection, out, (Stream) response);
                    return;
                }
                write(out, response, close);
                if (close) return;
            }
        } catch (IOException | RuntimeException e) {
            // The browser went away or sent nonsense; nothing to do
        }
    }

    private static void stream(Socket connection, OutputStream out, Stream response) throws IOException {
        connection.setSoTimeout(0);
        String head = "HTTP/1.1 200 OK\r\n"
                + "Content-Type: " + response.type + "\r\n"
                + "Cache-Control: no-store\r\n"
                + "Connection: close\r\n\r\n"
                // If the connection drops, the page reconnects after half a second (browsers wait ~3 s by default)
                + "retry: 500\n\n";
        out.write(head.getBytes(StandardCharsets.ISO_8859_1));
        out.flush();
        try {
            response.events.run(out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void write(OutputStream out, Response response, boolean close) throws IOException {
        byte[] body = response.body == null ? new byte[0] : response.body;
        StringBuilder head = new StringBuilder()
                .append("HTTP/1.1 ").append(response.code).append(' ').append(reason(response.code)).append("\r\n")
                .append("Content-Length: ").append(body.length).append("\r\n")
                .append("Cache-Control: no-store\r\n")
                .append(close ? "Connection: close\r\n" : "Connection: keep-alive\r\n");
        if (response.type != null) head.append("Content-Type: ").append(response.type).append("\r\n");
        head.append("\r\n");
        out.write(head.toString().getBytes(StandardCharsets.ISO_8859_1));
        out.write(body);
        out.flush();
    }

    private static String reason(int code) {
        switch (code) {
            case 200: return "OK";
            case 204: return "No Content";
            case 400: return "Bad Request";
            case 404: return "Not Found";
            case 405: return "Method Not Allowed";
            default: return "Error";
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null) return values;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            try {
                values.put(URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), "UTF-8"),
                        eq < 0 ? "" : URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }
        return values;
    }
}
