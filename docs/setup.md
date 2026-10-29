# Setup

Get from zero to deploying code on your robot. Pick **one** way to work:

| | What you install | Best for |
|---|---|---|
| **[A. VS Code dev container](#a-vs-code-dev-container-recommended)** (recommended) | Nothing by hand: `./open-workspace` sets everything up | Anyone, especially on a fresh computer |
| **[B. Android Studio](#b-android-studio)** | Android Studio | Teams already using Android Studio |
| **[C. VS Code without a container](#c-vs-code-without-a-container)** | JDK, Android SDK, VS Code | People who want everything installed locally |

First, create your repository:
1. On the main GitHub page for this repo, click **Use this template** → **Create a new repository** (or click **Fork**). Give it a name.
2. Click the green **Code** button on *your* new repository and copy the URL.

Once you're set up, try the **[hive game simulator](#try-it-without-a-robot-the-hive-game)**: drive a simulated robot through this season's game with the keyboard and watch its behavior tree live. No robot needed.

---

## A. VS Code dev container (recommended)

Java, the Android SDK and `adb` all live inside a container, so your computer only needs the tools that run it.

### macOS
```bash
git clone <your repository URL> ~/src/ftc-framework
```
```bash
cd ~/src/ftc-framework && ./open-workspace
```

`./open-workspace` runs [`scripts/bootstrap.sh`](../scripts/bootstrap.sh), which installs anything missing (asking first): Homebrew, Rosetta on Apple Silicon, [Colima](https://github.com/abiosoft/colima) (a small VM that runs the container), the Docker CLI, VS Code and its Dev Containers extension. It then starts the VM, downloads the container image, and opens VS Code inside the container.

The first run takes several minutes; later runs take seconds. When VS Code opens, the first container start also downloads the FTC libraries (watch the terminal it opens).

- Clone the repository somewhere **inside your home folder**: the VM can only see files there.
- When you are done for the day, free the VM's memory with `scripts/bootstrap.sh stop`.
- Your active Docker context is left alone: only this dev container runs on the `colima-ftc` VM.
- If the image can't be downloaded, `./open-workspace` builds it from the Dockerfile instead.
- Pass `--yes` to install missing tools without prompts.

### Windows and Linux
1. Install [Docker](https://docs.docker.com/get-docker/) and [VS Code](https://code.visualstudio.com) with the **Dev Containers** extension.
2. On Linux you can then run `./open-workspace`. On Windows, open the folder in VS Code and run **Dev Containers: Reopen in Container** from the Command Palette.

### Deploying from the container
The container can't see USB devices, so deploy over Wi-Fi:
1. Join your Control Hub's Wi-Fi network.
2. Run the **Connect to Control Hub (Wi-Fi)** task, then **Deploy to robot** (see [everyday use](#everyday-use-in-vs-code)).

---

## B. Android Studio
1. Open Android Studio and go to **File** → **New** → **Project from Version Control**. Paste in the URL and continue.
2. It may take a few minutes to sync and build. You are ready when the left-hand panel shows `robot`, `framework`, `pedro`, `FtcRobotController` and `Gradle Scripts`.
3. Plug in or connect to your Control Hub and press the green **Run** button to deploy.

To see how behavior trees run without a robot, pick the **Examples** run configuration instead (every laptop example in one page, picked from a drop-down) (see [`examples/`](../examples/README.md#running-the-behavior-tree-visualizer)), or **Hive game** to [drive a simulated robot](#try-it-without-a-robot-the-hive-game).

---

## C. VS Code without a container
**Prerequisites** (Android Studio installs these for you; without it, install them yourself):
- **JDK 17 or 21** (for example [Temurin](https://adoptium.net/)).
- **The Android SDK**, with `ANDROID_HOME` set to its location (Android Studio puts it at `~/Library/Android/sdk` on macOS, `%LOCALAPPDATA%\Android\Sdk` on Windows and `~/Android/Sdk` on Linux). Alternatively, create a `local.properties` file in the repo root containing `sdk.dir=<path to sdk>`.
- **`adb`** on your `PATH` (it lives in `<sdk>/platform-tools`) for deploying to the robot.

**Setup:**
1. Clone your repository and open the folder in VS Code (**File** → **Open Folder**).
2. Install the recommended **Extension Pack for Java** and **Gradle for Java** extensions when prompted.
3. Run **Terminal** → **Run Task…** → **Refresh VS Code classpath**. This downloads the FTC libraries and exports them so VS Code can autocomplete and check your code. Run it again whenever you change `build.dependencies.gradle`.

---

## Try it without a robot: the hive game
A simulator of this season's game runs on your computer. You drive the red robot with the keyboard while its TeleOp behavior tree shows live next to the field, so you can see which nodes run as you drive, collect and shoot. The blue robot drives itself with the Auto.

**1. Start it**, whichever way you set up:

| Where | How |
|-------|-----|
| VS Code (A or C) | **Run and Debug** (▷ in the sidebar) → **Hive game**, or **Terminal** → **Run Task…** → **Run hive game** |
| Android Studio (B) | Pick **Hive game** in the run configuration dropdown next to ▶ and press Run |
| Terminal | `./gradlew :examples:hive` |

**2. Open http://localhost:8765** (it opens by itself when not in a container), click the page, and press **Play**.

**3. Drive:**

| Keys | Does |
|------|------|
| <kbd>↑</kbd> <kbd>↓</kbd> | Drive forward / back |
| <kbd>←</kbd> <kbd>→</kbd> | Turn |
| <kbd>Q</kbd> <kbd>E</kbd> | Slide sideways |
| <kbd>Shift</kbd> | Drive slowly while held |
| <kbd>Space</kbd> | Intake, while held |
| <kbd>F</kbd> | Shoot (hold to keep shooting) |
| <kbd>A</kbd> | Line up a shot automatically, while held |
| <kbd>P</kbd> / <kbd>R</kbd> | Play-pause / restart |

Shoot into the cup at the **up** end of your hive: when one end is 20 units heavier (wiffle ball 1, softball 3) the hive pivots for +20 points. The dotted line in front of your robot turns green when a shot would land in the right cup. Use **⛶ Full screen** for just the field, and **Collapse** to fold the tree down to what matters. The full rules and how the tree is built are in [`examples/README.md`](../examples/README.md#the-hive-game).

**If it doesn't start:**
- *"Port 8765 is already in use"*: something else (often VS Code) has that port. Pick another and open that one instead: `./gradlew :examples:hive -Dvisualizer.port=8766`, then http://localhost:8766.
- *"Unsupported class file major version"*: your Java is newer than this Gradle supports. Use JDK 17 or 21 (see [option C](#c-vs-code-without-a-container)), for example by setting `JAVA_HOME` to it. The dev container (option A) already has the right one.

---

## Everyday use in VS Code
All under **Terminal** → **Run Task…**, in or out of the container:

| Task | What it does |
|------|--------------|
| **Build** (`Ctrl/Cmd+Shift+B`) | Compiles the Robot Controller app |
| **Connect to Control Hub (Wi-Fi)** | Connects `adb` to a Control Hub whose Wi-Fi network you have joined (not needed over USB) |
| **Deploy to robot** | Installs the app on the connected Control Hub/phone and starts it |
| **Test framework** | Runs the behavior tree and sdk unit tests (no robot needed) |
| **Run examples** | Every laptop example (behavior tree basics, hive game, tuning demo) in one page at http://localhost:8765, picked from a drop-down (see [`examples/`](../examples/README.md#running-the-behavior-tree-visualizer)) |
| **Run tuning demo** | The sdk's auto-tuner on a simulated lift, arm or flywheel (see [Tuning](../sdk/README.md#tuning)) |
| **Run hive game** | Drive a simulated robot through this season's game with the keyboard at http://localhost:8765 (see [above](#try-it-without-a-robot-the-hive-game)) |
| **Refresh VS Code classpath** | Updates VS Code's view of the libraries |
| **Clean** | Deletes build outputs |

The Gradle view in the sidebar lists every Gradle task too.

> VS Code only checks your code for errors as you type; the **Build** task is what actually compiles it with the FTC toolchain. If they ever disagree, trust **Build**.

---

## The dev container image
The container image is built from [`.devcontainer/Dockerfile`](../.devcontainer/Dockerfile) and published to `ghcr.io/greenland-robotics/ftc-framework/devcontainer` by the [**Dev container image**](../.github/workflows/devcontainer-image.yml) workflow. It rebuilds only when the Dockerfile (or the workflow) changes on `main`, and pull requests that change it are test-built without publishing. Tags: `latest` and `sha-<commit>`.

If you change the Dockerfile in your own repository, the workflow publishes to *your* `ghcr.io/<owner>/<repo>/devcontainer`; update the `image` in [`.devcontainer/devcontainer.json`](../.devcontainer/devcontainer.json) to match.
