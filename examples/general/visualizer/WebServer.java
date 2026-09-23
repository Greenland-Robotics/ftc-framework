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

    static final class Response {
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
            connection.setSoTimeout(5000);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(connection.getInputStream(), StandardCharsets.ISO_8859_1));
            String requestLine = in.readLine();
            if (requestLine == null) return;
            for (String header = in.readLine(); header != null && !header.isEmpty(); header = in.readLine()) {
                // Headers aren't needed; requests carry no body
            }

            String[] parts = requestLine.split(" ");
            Response response;
            if (parts.length < 2) {
                response = Response.text(400, "Bad request");
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
            write(connection.getOutputStream(), response);
        } catch (IOException e) {
            // The browser went away; nothing to do
        }
    }

    private static void write(OutputStream out, Response response) throws IOException {
        byte[] body = response.body == null ? new byte[0] : response.body;
        StringBuilder head = new StringBuilder()
                .append("HTTP/1.1 ").append(response.code).append(' ').append(reason(response.code)).append("\r\n")
                .append("Content-Length: ").append(body.length).append("\r\n")
                .append("Cache-Control: no-store\r\n")
                .append("Connection: close\r\n");
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
