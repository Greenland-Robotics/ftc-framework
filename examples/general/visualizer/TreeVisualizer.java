package visualizer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.BindException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import behavior.Node;
import behavior.Status;
import behavior.runtime.BehaviorRunner;
import visualizer.BehaviorTree.Info;
import visualizer.WebServer.Request;
import visualizer.WebServer.Response;

/**
 * Runs a behavior tree on this computer and shows it live in a web page, so you can watch which
 * nodes are ticked, step through it one tick at a time and see what each node printed.
 *
 * <pre>
 * new TreeVisualizer(MyExample::routine)
 *         .watch("arm position", () -> armPosition)
 *         .start();
 * </pre>
 *
 * It plays the role of the OpMode: it calls {@code runner.tick()} every loop, but only while you
 * press Play or Step. The routine supplier is called again on Restart, so it should also reset
 * any state.
 *
 * <p>For a simulated robot it can also draw a {@link #scene scene} (like the field), pass
 * {@link #key keys} held in the page to the program (to drive with the keyboard), and offer
 * {@link #setting settings} that restart the routine.
 *
 * <p>One page can hold several programs: give each one to {@link #example} and the page gets an
 * "Example" drop-down to switch between them (see Examples.java).
 */
public final class TreeVisualizer implements BehaviorTree.Listener {
    public static final int DEFAULT_PORT = 8765;

    private static final int LOG_LIMIT = 300;
    private static final int NODE_OUTPUT_LIMIT = 3;

    private static final String EXAMPLE_SETTING = "Example";

    private Supplier<Node> routine;
    private final Map<String, Consumer<TreeVisualizer>> examples = new LinkedHashMap<>();
    private final Map<String, Supplier<?>> watches = new LinkedHashMap<>();
    private final List<Runnable> afterTicks = new ArrayList<>();
    private final List<KeyHelp> keys = new ArrayList<>();
    private final Map<String, Setting> settings = new LinkedHashMap<>();
    private Supplier<String> scene;
    private int port = Integer.getInteger("visualizer.port", DEFAULT_PORT);   // -Dvisualizer.port=8766

    // Everything below is guarded by `lock`: the loop thread runs the tree, web requests read and control it
    private final Object lock = new Object();
    private Thread loopThread;
    private BehaviorTree tree;
    private BehaviorRunner runner;
    private int generation;          // Bumped on restart, so the page knows to rebuild the tree
    private boolean finished;
    private Status result;
    private int ticks;
    private long startNanos;
    private long finishNanos;
    private String lastAction;
    private String error;
    private int errorNode = -1;
    private boolean playing;
    private int stepsRequested;
    private boolean restartRequested;
    private int periodMs = 20;
    private final Deque<Info> calling = new ArrayDeque<>();
    private final List<LogLine> log = new ArrayList<>();
    private final Set<String> held = new HashSet<>();
    private long heldStamp;          // The page's timestamp for `held`, to ignore updates that arrive late
    private long heldNanos;          // When `held` was last updated

    /** Keys held this long without word from the page are let go (e.g. the tab was closed). */
    private static final long KEY_TIMEOUT_NANOS = 2_000_000_000L;

    /** How often the page is sent the tree's state, and the scene while playing and while paused. */
    private static final long STATE_MS = 100;
    private static final long SCENE_PLAYING_MS = 33;
    private static final long SCENE_PAUSED_MS = 200;

    private static final class KeyHelp {
        final String help;
        final String[] codes;

        KeyHelp(String help, String[] codes) {
            this.help = help;
            this.codes = codes;
        }
    }

    private static final class Setting {
        final String[] options;
        final Consumer<String> onChange;
        String value;

        Setting(String[] options, Consumer<String> onChange) {
            this.options = options;
            this.onChange = onChange;
            this.value = options[0];
        }
    }

    private static final class LogLine {
        final int tick;
        final long ms;
        final int node;
        final String text;

        LogLine(int tick, long ms, int node, String text) {
            this.tick = tick;
            this.ms = ms;
            this.node = node;
            this.text = text;
        }
    }

    public TreeVisualizer(Supplier<Node> routine) {
        this.routine = routine;
    }

    /** A visualizer whose programs are added with {@link #example}. */
    public TreeVisualizer() {
        this(null);
    }

    /** The tree to run. Restart calls {@code routine} again, so it should also reset any state. */
    public TreeVisualizer routine(Supplier<Node> routine) {
        this.routine = routine;
        return this;
    }

    /**
     * Adds a program to the page's "Example" drop-down. {@code setUp} sets the visualizer up the
     * way that program's own main would ({@link #routine}, {@link #watch}, {@link #scene},
     * {@link #key}, {@link #setting}...); picking it in the page clears the last program's and
     * calls {@code setUp} again. The first one added starts.
     */
    public TreeVisualizer example(String name, Consumer<TreeVisualizer> setUp) {
        examples.put(name, setUp);
        return this;
    }

    /** Shows a value (like a motor position) next to the tree, updated live. */
    public TreeVisualizer watch(String name, Supplier<?> value) {
        watches.put(name, value);
        return this;
    }

    /** Runs {@code action} after every tick, e.g. to pretend the world changed. */
    public TreeVisualizer afterEachTick(Runnable action) {
        afterTicks.add(action);
        return this;
    }

    /**
     * Draws a picture next to the tree, such as the field and the robot, refreshed many times a
     * second. {@code json} returns {@code {"width": w, "height": h, "shapes": [...]}} in any units
     * (y points up); see index.html for the shapes it can draw. Runs on the tree's thread.
     */
    public TreeVisualizer scene(Supplier<String> json) {
        this.scene = json;
        return this;
    }

    /**
     * Passes these keys to the program while the page has focus, instead of the page's own
     * shortcuts, and lists them under Controls. Keys are {@code KeyboardEvent.code} values, like
     * {@code "ArrowUp"}, {@code "Space"} or {@code "KeyF"}. Read them with {@link #isHeld}.
     */
    public TreeVisualizer key(String help, String... codes) {
        keys.add(new KeyHelp(help, codes));
        return this;
    }

    /** Whether a key passed with {@link #key} is held down in the page right now. */
    public boolean isHeld(String code) {
        synchronized (lock) {
            return held.contains(code);
        }
    }

    /**
     * A drop-down in the page; picking an option calls {@code onChange} with it and restarts.
     * The first option is the starting one; {@code onChange} is not called for it.
     */
    public TreeVisualizer setting(String name, Consumer<String> onChange, String... options) {
        settings.put(name, new Setting(options, onChange));
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
            if (!examples.isEmpty()) {
                String[] names = examples.keySet().toArray(new String[0]);
                settings.put(EXAMPLE_SETTING, new Setting(names, this::useExample));
                useExample(names[0]);
            }
            reset();
        }

        boolean inContainer = new File("/.dockerenv").exists() || System.getenv("REMOTE_CONTAINERS") != null;
        WebServer server;
        try {
            // Inside a dev container, listen on every interface so VS Code's port forwarding can reach it
            server = new WebServer(inContainer ? null : InetAddress.getLoopbackAddress(), port, this::handle);
        } catch (BindException e) {
            throw new IOException("Port " + port + " is already in use. Is the visualizer already running?", e);
        }
        server.start();

        String url = "http://localhost:" + port;
        console.println();
        console.println("Behavior tree visualizer: open " + url + " in your browser.");
        console.println("Press Play or Step there to run the tree. Stop this program to quit.");
        console.println();
        if (!inContainer && !"false".equals(System.getProperty("visualizer.browser"))) {
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
                if (!held.isEmpty() && now - heldNanos > KEY_TIMEOUT_NANOS) held.clear();
                if (stepsRequested > 0) {
                    stepsRequested--;
                    tick();
                } else if (playing && now >= nextTick) {
                    tick();
                    // Keep a steady rate (a late tick doesn't push back the rest), but don't rush to
                    // catch up after a pause or a long hiccup
                    long period = periodMs * 1_000_000L;
                    nextTick = nextTick + period < now - 5 * period ? now + period : nextTick + period;
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

    /** Clears the current program's setup and sets up {@code name}'s in its place. */
    private void useExample(String name) {
        Setting picker = settings.get(EXAMPLE_SETTING);
        settings.clear();
        settings.put(EXAMPLE_SETTING, picker);
        watches.clear();
        afterTicks.clear();
        keys.clear();
        held.clear();
        scene = null;
        routine = null;
        playing = false;
        examples.get(name).accept(this);
    }

    private void reset() {
        calling.clear();
        log.clear();
        finished = false;
        result = null;
        ticks = 0;
        error = null;
        errorNode = -1;
        generation++;
        lastAction = "Nothing yet. Each step calls runner.tick() once.";
        tree = null;
        try {
            if (routine == null) throw new IllegalStateException("no routine: call routine(...) or pass one to the constructor");
            tree = new BehaviorTree(routine.get(), this);
            runner = new BehaviorRunner();
            runner.run(tree.root);
        } catch (RuntimeException e) {
            fail("Building the routine", e);
        }
    }

    private void tick() {
        if (finished || error != null || tree == null) return;
        if (ticks == 0) startNanos = System.nanoTime();
        ticks++;
        try {
            runner.tick();
            for (Runnable action : afterTicks) action.run();
        } catch (RuntimeException e) {
            fail("Running the tree", e);
            calling.clear();
            return;
        }
        String status = tree.root.info.state.toUpperCase(Locale.ROOT);
        lastAction = "runner.tick() #" + ticks + ": the root returned " + status;
        if (runner.isIdle()) {
            finished = true;
            result = Status.valueOf(status);
            finishNanos = System.nanoTime();
            lastAction += ", so the runner is done with it";
        }
    }

    private void fail(String doing, RuntimeException e) {
        error = doing + " threw " + e;
        e.printStackTrace();
    }

    @Override
    public int currentTick() {
        return ticks;
    }

    @Override
    public void enter(Info info) {
        calling.push(info);
    }

    @Override
    public void exit() {
        calling.pop();
    }

    @Override
    public void threw(Info info) {
        if (errorNode < 0) errorNode = info.id;   // The innermost node is the one that threw
    }

    private long elapsedMs() {
        if (ticks == 0) return 0;
        return ((finished ? finishNanos : System.nanoTime()) - startNanos) / 1_000_000L;
    }

    /** Lines printed while the tree runs, credited to the node that printed them. */
    private void printed(String text) {
        Info info = calling.peek();
        if (info != null) {
            info.output.add(text);
            if (info.output.size() > NODE_OUTPUT_LIMIT) info.output.remove(0);
        }
        log.add(new LogLine(ticks, elapsedMs(), info == null ? -1 : info.id, text));
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

    private Response handle(Request request) throws IOException {
        switch (request.path) {
            case "/":
            case "/index.html":
                return new Response(200, "text/html; charset=utf-8", page());
            case "/api/state":
                String json;
                synchronized (lock) {
                    json = state();
                }
                return new Response(200, "application/json", json.getBytes(StandardCharsets.UTF_8));
            case "/api/scene":
                String picture;
                synchronized (lock) {
                    picture = sceneJson();
                }
                return new Response(200, "application/json",
                        (picture == null ? "null" : picture).getBytes(StandardCharsets.UTF_8));
            case "/api/stream":
                // The state and the scene, pushed over one connection as they change
                return new WebServer.Stream(this::stream);
            case "/api/control":
                if (!request.method.equals("POST")) return Response.text(405, "Use POST");
                return control(request.query.getOrDefault("action", ""), request.query.getOrDefault("value", ""),
                        request.query);
            default:
                return Response.text(404, "Not found");
        }
    }

    private void stream(OutputStream out) throws IOException, InterruptedException {
        long nextState = 0;
        while (true) {
            String stateJson = null;
            String sceneJson;
            boolean isPlaying;
            long now = System.nanoTime() / 1_000_000L;
            synchronized (lock) {
                if (now >= nextState) stateJson = state();
                sceneJson = sceneJson();
                isPlaying = playing;
            }
            // Written outside the lock, so a slow connection never holds up the tree
            if (stateJson != null) {
                WebServer.send(out, "state", stateJson);
                nextState = now + STATE_MS;
            }
            if (sceneJson != null) WebServer.send(out, "scene", sceneJson);
            Thread.sleep(sceneJson != null && isPlaying ? SCENE_PLAYING_MS
                    : sceneJson != null ? SCENE_PAUSED_MS : STATE_MS);
        }
    }

    /** The scene's JSON, or null if there is no scene (or drawing it failed). */
    private String sceneJson() {
        if (scene == null) return null;
        try {
            return scene.get();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private Response control(String action, String value, Map<String, String> query) {
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
                case "keys":
                    long stamp = parseLong(query.get("stamp"));
                    if (stamp < heldStamp) break;   // An older update that arrived late
                    heldStamp = stamp;
                    heldNanos = System.nanoTime();
                    held.clear();
                    for (String code : value.split(",")) {
                        if (!code.isEmpty()) held.add(code);
                    }
                    break;
                case "setting":
                    Setting setting = settings.get(query.getOrDefault("name", ""));
                    if (setting == null || !java.util.Arrays.asList(setting.options).contains(value)) {
                        return Response.text(400, "Unknown setting");
                    }
                    setting.value = value;
                    setting.onChange.accept(value);
                    restartRequested = true;
                    break;
                case "period":
                    try {
                        periodMs = Math.max(1, Math.min(5000, Integer.parseInt(value)));
                    } catch (NumberFormatException ignored) {
                        // Keep the current speed
                    }
                    break;
                default:
                    return Response.text(400, "Unknown action " + action);
            }
            lock.notifyAll();
        }
        return new Response(204, null, null);
    }

    private String state() {
        Json json = new Json().begin();
        json.field("generation", generation)
                .field("started", ticks > 0)
                .field("finished", finished)
                .field("result", result == null ? null : result.name().toLowerCase(Locale.ROOT))
                .field("playing", playing)
                .field("ticks", ticks)
                .field("elapsedMs", elapsedMs())
                .field("periodMs", periodMs)
                .field("lastAction", lastAction)
                .field("error", error)
                .field("errorNode", errorNode)
                .field("hasScene", scene != null);

        json.name("keys").beginArray();
        for (KeyHelp key : keys) {
            json.begin().field("help", key.help).name("codes").beginArray();
            for (String code : key.codes) json.value(code);
            json.endArray().end();
        }
        json.endArray();

        json.name("settings").beginArray();
        for (Map.Entry<String, Setting> entry : settings.entrySet()) {
            json.begin().field("name", entry.getKey()).field("value", entry.getValue().value).name("options").beginArray();
            for (String option : entry.getValue().options) json.value(option);
            json.endArray().end();
        }
        json.endArray();

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
            json.begin().field("tick", line.tick).field("ms", line.ms).field("node", line.node)
                    .field("text", line.text).end();
        }
        json.endArray();

        json.name("root");
        if (tree == null) {
            json.nullValue();
        } else {
            node(json, tree.root.info, 0);
        }
        return json.end().toString();
    }

    private void node(Json json, Info info, int depth) {
        Long ms = null;
        if (info.state.equals("running")) ms = (System.nanoTime() - info.startNanos) / 1_000_000L;
        else if (info.runs > 0) ms = (info.endNanos - info.startNanos) / 1_000_000L;

        json.begin()
                .field("id", info.id)
                .field("type", info.type())
                .field("name", info.name)
                .field("kind", BehaviorTree.kind(info))
                .field("label", info.label)
                .field("description", BehaviorTree.describe(info))
                .field("state", info.state)
                .field("tickedNow", ticks > 0 && info.lastTick == ticks)
                .field("ticks", info.ticks)
                .field("runs", info.runs)
                .field("halts", info.halts)
                .field("ms", ms);
        json.name("output").beginArray();
        for (String line : info.output) json.value(line);
        json.endArray();

        json.name("children").beginArray();
        if (depth < 64) {   // Guards against a node that contains itself
            for (Info child : info.children) node(json, child, depth + 1);
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

    private static long parseLong(String text) {
        try {
            return text == null ? 0 : Long.parseLong(text);
        } catch (NumberFormatException e) {
            return 0;
        }
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
