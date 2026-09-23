package visualizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import commands.Command;
import commands.CommandRunner;
import visualizer.CommandTree.Node;
import visualizer.CommandTree.Status;

/**
 * Runs a command tree on this computer and shows it live in a web page, so you can watch
 * which commands are running, step through it one update() at a time and see what each
 * command printed.
 *
 * <pre>
 * new TreeVisualizer(MyExample::routine)
 *         .watch("arm position", () -> armPosition)
 *         .start();
 * </pre>
 *
 * It plays the role of the OpMode: it calls {@code commandRunner.start()} once and then
 * {@code commandRunner.update()} every tick, but only while you press Play or Step.
 * The routine supplier is called again on Restart, so it should also reset any state.
 *
 * Desktop only: it uses the JDK's built-in web server, which Android doesn't have.
 */
public final class TreeVisualizer implements CommandTree.Listener {
    public static final int DEFAULT_PORT = 8765;

    private static final int LOG_LIMIT = 300;
    private static final int NODE_OUTPUT_LIMIT = 3;

    private final Supplier<Command> routine;
    private final Map<String, Supplier<?>> watches = new LinkedHashMap<>();
    private int port = DEFAULT_PORT;

    // Everything below is guarded by `lock`: the loop thread runs the commands, web requests read and control them
    private final Object lock = new Object();
    private Thread loopThread;
    private CommandTree tree;
    private CommandRunner runner;
    private int generation;          // Bumped on restart, so the page knows to rebuild the tree
    private boolean started;
    private boolean finished;
    private int updates;
    private long startNanos;
    private long finishNanos;
    private String lastAction;
    private String error;
    private int errorNode = -1;
    private boolean playing;
    private int stepsRequested;
    private boolean restartRequested;
    private int periodMs = 20;
    private final Deque<Node> calling = new ArrayDeque<>();
    private final List<LogLine> log = new ArrayList<>();

    private static final class LogLine {
        final int update;
        final long ms;
        final int node;
        final String text;

        LogLine(int update, long ms, int node, String text) {
            this.update = update;
            this.ms = ms;
            this.node = node;
            this.text = text;
        }
    }

    public TreeVisualizer(Supplier<Command> routine) {
        this.routine = routine;
    }

    /** Shows a value (like a motor position) next to the tree, updated live. */
    public TreeVisualizer watch(String name, Supplier<?> value) {
        watches.put(name, value);
        return this;
    }

    public TreeVisualizer port(int port) {
        this.port = port;
        return this;
    }

    /** Starts the web page and runs the tree until the program is stopped. Never returns. */
    public void start() throws IOException, InterruptedException {
        PrintStream console = System.out;
        loopThread = Thread.currentThread();
        System.setOut(new PrintStream(new CapturedOutput(console), true));

        synchronized (lock) {
            reset();
        }

        boolean inContainer = new File("/.dockerenv").exists() || System.getenv("REMOTE_CONTAINERS") != null;
        // Inside a dev container, listen on every interface so VS Code's port forwarding can reach it
        InetSocketAddress address = inContainer
                ? new InetSocketAddress(port)
                : new InetSocketAddress(InetAddress.getLoopbackAddress(), port);
        HttpServer server;
        try {
            server = HttpServer.create(address, 0);
        } catch (BindException e) {
            throw new IOException("Port " + port + " is already in use. Is the visualizer already running?", e);
        }
        server.createContext("/", this::handlePage);
        server.createContext("/api/state", this::handleState);
        server.createContext("/api/control", this::handleControl);
        server.start();

        String url = "http://localhost:" + port;
        console.println();
        console.println("Command tree visualizer: open " + url + " in your browser.");
        console.println("Press Play or Step there to run the tree. Stop this program to quit.");
        console.println();
        if (!inContainer) {
            openBrowser(url);
        }

        runLoop();
    }

    // ---- Running the tree ----

    private void runLoop() throws InterruptedException {
        long nextTick = 0;
        synchronized (lock) {
            while (true) {
                if (restartRequested) {
                    restartRequested = false;
                    reset();
                }
                long now = System.nanoTime();
                if (stepsRequested > 0) {
                    stepsRequested--;
                    tick();
                } else if (playing && now >= nextTick) {
                    tick();
                    nextTick = now + periodMs * 1_000_000L;
                }
                if (finished || error != null) {
                    playing = false;
                    stepsRequested = 0;
                }

                if (stepsRequested > 0) continue;
                // Sleep until the next tick, or until the page presses a button (0 = no timeout)
                lock.wait(playing ? Math.max(1, (nextTick - System.nanoTime()) / 1_000_000L) : 0);
            }
        }
    }

    private void reset() {
        calling.clear();
        log.clear();
        started = false;
        finished = false;
        updates = 0;
        error = null;
        errorNode = -1;
        generation++;
        lastAction = "Nothing yet. The first step calls commandRunner.start().";
        tree = null;
        try {
            tree = new CommandTree(routine.get(), this);
        } catch (RuntimeException e) {
            fail("Building the routine", e);
        }
    }

    private void tick() {
        if (finished || error != null || tree == null) return;
        try {
            if (!started) {
                started = true;
                startNanos = System.nanoTime();
                runner = new CommandRunner(tree.root);
                lastAction = "commandRunner.start(): init() on the root command";
                runner.start();
            } else {
                updates++;
                lastAction = "commandRunner.update() #" + updates + ": loop() then isFinished() on what is running";
                runner.update();
            }
        } catch (RuntimeException e) {
            fail("Running the tree", e);
            calling.clear();
            return;
        }
        if (runner.isFinished()) {
            finished = true;
            finishNanos = System.nanoTime();
            lastAction = "The root command finished, so commandRunner.isFinished() is true";
        }
    }

    private void fail(String doing, RuntimeException e) {
        error = doing + " threw " + e;
        e.printStackTrace();
    }

    @Override
    public void enter(Node node) {
        calling.push(node);
    }

    @Override
    public void exit() {
        calling.pop();
    }

    @Override
    public void threw(Node node) {
        if (errorNode < 0) errorNode = node.id;   // The innermost command is the one that threw
    }

    private long elapsedMs() {
        if (!started) return 0;
        return ((finished ? finishNanos : System.nanoTime()) - startNanos) / 1_000_000L;
    }

    /** Lines printed while the tree runs, credited to the command that printed them. */
    private void printed(String text) {
        Node node = calling.peek();
        if (node != null) {
            node.output.add(text);
            if (node.output.size() > NODE_OUTPUT_LIMIT) node.output.remove(0);
        }
        log.add(new LogLine(updates, elapsedMs(), node == null ? -1 : node.id, text));
        if (log.size() > LOG_LIMIT) log.remove(0);
    }

    /** Passes System.out through to the console and collects the lines printed by the loop thread. */
    private final class CapturedOutput extends OutputStream {
        private final OutputStream console;
        private final ByteArrayOutputStream line = new ByteArrayOutputStream();

        CapturedOutput(OutputStream console) {
            this.console = console;
        }

        @Override
        public void write(int b) throws IOException {
            console.write(b);
            if (Thread.currentThread() != loopThread) return;
            if (b == '\n') {
                String text = new String(line.toByteArray(), StandardCharsets.UTF_8).replace("\r", "");
                line.reset();
                synchronized (lock) {
                    printed(text);
                }
            } else {
                line.write(b);
            }
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            for (int i = offset; i < offset + length; i++) write(bytes[i]);
        }

        @Override
        public void flush() throws IOException {
            console.flush();
        }
    }

    // ---- Web page ----

    private void handlePage(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (!path.equals("/") && !path.equals("/index.html")) {
            send(exchange, 404, "text/plain", "Not found".getBytes(StandardCharsets.UTF_8));
            return;
        }
        send(exchange, 200, "text/html; charset=utf-8", page());
    }

    private void handleState(HttpExchange exchange) throws IOException {
        String json;
        synchronized (lock) {
            json = state();
        }
        send(exchange, 200, "application/json", json.getBytes(StandardCharsets.UTF_8));
    }

    private void handleControl(HttpExchange exchange) throws IOException {
        if (!exchange.getRequestMethod().equals("POST")) {
            send(exchange, 405, "text/plain", "Use POST".getBytes(StandardCharsets.UTF_8));
            return;
        }
        Map<String, String> query = query(exchange.getRequestURI().getRawQuery());
        String action = query.getOrDefault("action", "");
        synchronized (lock) {
            switch (action) {
                case "play":
                    if (!finished && error == null) playing = true;
                    break;
                case "pause":
                    playing = false;
                    break;
                case "step":
                    playing = false;
                    stepsRequested++;
                    break;
                case "restart":
                    restartRequested = true;
                    break;
                case "period":
                    try {
                        periodMs = Math.max(1, Math.min(5000, Integer.parseInt(query.getOrDefault("value", ""))));
                    } catch (NumberFormatException ignored) {
                        // Keep the current speed
                    }
                    break;
                default:
                    send(exchange, 400, "text/plain", ("Unknown action " + action).getBytes(StandardCharsets.UTF_8));
                    return;
            }
            lock.notifyAll();
        }
        send(exchange, 204, null, null);
    }

    private String state() {
        Json json = new Json().begin();
        json.field("generation", generation)
                .field("started", started)
                .field("finished", finished)
                .field("playing", playing)
                .field("updates", updates)
                .field("elapsedMs", elapsedMs())
                .field("periodMs", periodMs)
                .field("lastAction", lastAction)
                .field("error", error)
                .field("errorNode", errorNode);

        json.name("watches").beginArray();
        for (Map.Entry<String, Supplier<?>> watch : watches.entrySet()) {
            String value;
            try {
                value = format(watch.getValue().get());
            } catch (RuntimeException e) {
                value = e.toString();
            }
            json.begin().field("name", watch.getKey()).field("value", value).end();
        }
        json.endArray();

        json.name("log").beginArray();
        for (LogLine line : log) {
            json.begin().field("update", line.update).field("ms", line.ms).field("node", line.node)
                    .field("text", line.text).end();
        }
        json.endArray();

        json.name("root");
        if (tree == null) {
            json.nullValue();
        } else {
            node(json, tree.root.node, false, 0);
        }
        return json.end().toString();
    }

    /**
     * Writes a node and its children. A command whose parent is done but that never started was
     * skipped; one that was still running was interrupted (e.g. by a TimeoutCommand).
     */
    private void node(Json json, Node node, boolean parentDone, int depth) {
        String status = node.status.name().toLowerCase(Locale.ROOT);
        if (parentDone && node.status == Status.PENDING) status = "skipped";
        if (parentDone && node.status == Status.RUNNING) status = "interrupted";

        Long ms = null;
        if (node.status == Status.RUNNING && !parentDone) ms = (System.nanoTime() - node.startNanos) / 1_000_000L;
        if (node.status == Status.FINISHED) ms = (node.endNanos - node.startNanos) / 1_000_000L;

        json.begin()
                .field("id", node.id)
                .field("type", node.type())
                .field("kind", CommandTree.kind(node))
                .field("label", node.label)
                .field("description", CommandTree.describe(node))
                .field("status", status)
                .field("inits", node.inits)
                .field("loops", node.loops)
                .field("ms", ms);
        json.name("output").beginArray();
        for (String line : node.output) json.value(line);
        json.endArray();

        json.name("children").beginArray();
        if (depth < 64) {   // Guards against a command that contains itself
            boolean done = !status.equals("pending") && !status.equals("running");
            Node skipped = CommandTree.skippedBranch(node);
            for (Node child : node.children) {
                node(json, child, done || child == skipped, depth + 1);
            }
        }
        json.endArray();
        json.end();
    }

    // ---- Helpers ----

    private byte[] page() throws IOException {
        InputStream in = TreeVisualizer.class.getResourceAsStream("index.html");
        if (in == null) {
            // Some IDEs don't copy non-Java files to their output folder; read it from the source instead
            return Files.readAllBytes(Paths.get("examples/general/visualizer/index.html"));
        }
        try (InputStream page = in) {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            for (int n; (n = page.read(buffer)) > 0; ) bytes.write(buffer, 0, n);
            return bytes.toByteArray();
        }
    }

    private static void send(HttpExchange exchange, int code, String type, byte[] body) throws IOException {
        if (type != null) exchange.getResponseHeaders().set("Content-Type", type);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(code, body == null ? -1 : body.length);
        if (body != null) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        }
        exchange.close();
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> values = new LinkedHashMap<>();
        if (raw == null) return values;
        for (String pair : raw.split("&")) {
            int eq = pair.indexOf('=');
            try {
                values.put(java.net.URLDecoder.decode(eq < 0 ? pair : pair.substring(0, eq), "UTF-8"),
                        eq < 0 ? "" : java.net.URLDecoder.decode(pair.substring(eq + 1), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
        }
        return values;
    }

    private static String format(Object value) {
        if (value instanceof Double || value instanceof Float) {
            String text = String.format(Locale.ROOT, "%.3f", ((Number) value).doubleValue());
            return text.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return String.valueOf(value);
    }

    private static void openBrowser(String url) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String[] command = os.contains("mac") ? new String[] {"open", url}
                : os.contains("win") ? new String[] {"rundll32", "url.dll,FileProtocolHandler", url}
                : new String[] {"xdg-open", url};
        try {
            new ProcessBuilder(command).start();
        } catch (IOException | RuntimeException ignored) {
            // No browser to open: the URL is printed above
        }
    }
}
