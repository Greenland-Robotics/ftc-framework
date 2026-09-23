# Build environment for the FTC robot app: JDK 17, the Android SDK and adb.
#
# The image is built for linux/amd64 (devcontainer.json passes --platform) because Android's build tools (aapt2) only ship for x86-64 Linux.
# On Apple Silicon, Colima runs it through Rosetta (see scripts/bootstrap.sh).
FROM mcr.microsoft.com/devcontainers/java:17-bookworm

ARG CMDLINE_TOOLS_VERSION=11076708
ENV ANDROID_HOME=/opt/android-sdk
ENV PATH=${PATH}:${ANDROID_HOME}/cmdline-tools/latest/bin:${ANDROID_HOME}/platform-tools

RUN apt-get update \
    && apt-get install -y --no-install-recommends unzip \
    && rm -rf /var/lib/apt/lists/*

# Match compileSdk and the Android Gradle Plugin's default build tools (build.common.gradle)
RUN mkdir -p ${ANDROID_HOME}/cmdline-tools \
    && curl -fsSL -o /tmp/cmdline-tools.zip \
        https://dl.google.com/android/repository/commandlinetools-linux-${CMDLINE_TOOLS_VERSION}_latest.zip \
    && unzip -q /tmp/cmdline-tools.zip -d /tmp \
    && mv /tmp/cmdline-tools ${ANDROID_HOME}/cmdline-tools/latest \
    && rm /tmp/cmdline-tools.zip \
    && yes | sdkmanager --licenses > /dev/null \
    && sdkmanager --install "platforms;android-34" "build-tools;34.0.0" "platform-tools" \
    && chown -R vscode:vscode ${ANDROID_HOME}

# Owned by vscode so the Gradle cache volume mounted here is writable
RUN mkdir -p /home/vscode/.gradle && chown vscode:vscode /home/vscode/.gradle
