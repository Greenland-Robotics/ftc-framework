#!/usr/bin/env bash
#
# Sets up and manages what your computer needs to run the dev container:
# Homebrew, Colima (a lightweight Linux VM that runs Docker), the Docker CLI,
# VS Code and its Dev Containers extension. Java and the Android SDK live in the container.
#
# Usage: scripts/bootstrap.sh <command> [--yes]
#   start           Install anything missing and start the "ftc" Colima VM
#   stop            Stop the VM
#   status          Show whether the VM is running
#   code-path       Print the path of the VS Code command-line tool
#   docker-context  Print the Docker context the dev container uses (empty: Docker's default)
#
# Your active Docker context is never changed: open-workspace tells VS Code to use the
# "colima-ftc" context for this dev container only.
#
# --yes (or FTC_YES=1) installs missing tools without asking.
# FTC_COLIMA_CPUS, FTC_COLIMA_MEMORY (GiB) and FTC_COLIMA_DISK (GiB) override the VM size.

set -euo pipefail

PROFILE=ftc
CONTEXT="colima-$PROFILE"
ASSUME_YES="${FTC_YES:-0}"

info() { printf '\033[1;34m==>\033[0m %s\n' "$*" >&2; }
warn() { printf '\033[1;33mWarning:\033[0m %s\n' "$*" >&2; }
die()  { printf '\033[1;31mError:\033[0m %s\n' "$*" >&2; exit 1; }

confirm() {
    [[ "$ASSUME_YES" == 1 ]] && return 0
    [[ -r /dev/tty ]] || die "$1 Re-run with --yes to allow this without a prompt."
    local reply
    read -r -p "$1 [Y/n] " reply < /dev/tty
    [[ -z "$reply" || "$reply" =~ ^[Yy] ]]
}

is_macos() { [[ "$(uname -s)" == Darwin ]]; }
is_apple_silicon() { is_macos && [[ "$(uname -m)" == arm64 ]]; }

# --- Tools ---------------------------------------------------------------------------------

ensure_homebrew() {
    if ! command -v brew > /dev/null; then
        for prefix in /opt/homebrew /usr/local; do
            [[ -x "$prefix/bin/brew" ]] && eval "$("$prefix/bin/brew" shellenv)" && return
        done
        confirm "Homebrew (the macOS package manager) is needed to install the tools. Install it?" \
            || die "Homebrew is required: see https://brew.sh"
        /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
        for prefix in /opt/homebrew /usr/local; do
            [[ -x "$prefix/bin/brew" ]] && eval "$("$prefix/bin/brew" shellenv)" && return
        done
        die "Homebrew was installed but could not be found. Open a new terminal and try again."
    fi
}

ensure_rosetta() {
    # The container is x86-64 (Android's build tools don't support ARM Linux); Rosetta runs it fast
    is_apple_silicon || return 0
    arch -x86_64 /usr/bin/true 2> /dev/null && return
    confirm "Rosetta is needed to run the x86-64 dev container. Install it (asks for your password)?" \
        || die "Rosetta is required on Apple Silicon Macs."
    sudo softwareupdate --install-rosetta --agree-to-license
}

brew_install() {
    local command=$1 package=$2 flags=${3:-}
    command -v "$command" > /dev/null && return
    confirm "$package is not installed. Install it with Homebrew?" || die "$package is required."
    # shellcheck disable=SC2086
    brew install $flags "$package"
}

code_path() {
    if command -v code > /dev/null; then
        command -v code
        return
    fi
    local app
    for app in "/Applications/Visual Studio Code.app" "$HOME/Applications/Visual Studio Code.app"; do
        if [[ -x "$app/Contents/Resources/app/bin/code" ]]; then
            echo "$app/Contents/Resources/app/bin/code"
            return
        fi
    done
    return 1
}

ensure_vscode() {
    if ! code_path > /dev/null; then
        is_macos || die "Install VS Code (https://code.visualstudio.com) and make sure 'code' is on your PATH."
        confirm "VS Code is not installed. Install it with Homebrew?" || die "VS Code is required."
        brew install --cask visual-studio-code
    fi
    local code
    code="$(code_path)" || die "VS Code was installed but its 'code' command could not be found."
    local extensions
    extensions="$("$code" --list-extensions)"
    if ! grep -qix ms-vscode-remote.remote-containers <<< "$extensions"; then
        info "Installing the VS Code Dev Containers extension"
        "$code" --install-extension ms-vscode-remote.remote-containers > /dev/null
    fi
}

check_docker_credentials() {
    # A leftover Docker Desktop config breaks image pulls once Docker Desktop is gone
    local config="$HOME/.docker/config.json"
    if [[ -f "$config" ]] && grep -q '"credsStore": *"desktop"' "$config" \
        && ! command -v docker-credential-desktop > /dev/null; then
        warn "$config uses Docker Desktop's credential store, which isn't installed."
        warn "If pulling the image fails, remove the \"credsStore\" line from that file."
    fi
}

# --- Colima VM -----------------------------------------------------------------------------

vm_exists() {
    local profiles
    profiles="$(colima list 2> /dev/null | awk 'NR > 1 { print $1 }')"
    grep -qx "$PROFILE" <<< "$profiles"
}
vm_running() { colima status "$PROFILE" > /dev/null 2>&1; }

default_memory() {
    # Half the computer's memory, between 4 and 8 GiB
    local total=$(( $(sysctl -n hw.memsize) / 1024 / 1024 / 1024 / 2 ))
    (( total < 4 )) && total=4
    (( total > 8 )) && total=8
    echo "$total"
}

start_vm() {
    if vm_running; then
        info "Colima VM '$PROFILE' is already running"
        return
    fi
    if vm_exists; then
        info "Starting Colima VM '$PROFILE'"
        colima start "$PROFILE" --activate=false
        return
    fi
    local args=(
        --activate=false
        --cpu "${FTC_COLIMA_CPUS:-4}"
        --memory "${FTC_COLIMA_MEMORY:-$(default_memory)}"
        --disk "${FTC_COLIMA_DISK:-60}"
    )
    if is_apple_silicon; then
        args+=(--arch aarch64 --vm-type vz --vz-rosetta)
    fi
    info "Creating Colima VM '$PROFILE' (first time only, takes a minute)"
    colima start "$PROFILE" "${args[@]}"
}

check_repo_location() {
    # Colima shares only your home folder with the VM
    local repo
    repo="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
    case "$repo/" in
        "$HOME"/*) ;;
        *) warn "$repo is outside your home folder, so the container can't see it. Clone the repository under $HOME." ;;
    esac
}

# --- Commands ------------------------------------------------------------------------------

cmd_start() {
    if is_macos; then
        ensure_homebrew
        ensure_rosetta
        brew_install colima colima
        brew_install docker docker
        ensure_vscode
        check_repo_location
        check_docker_credentials
        start_vm
    else
        # Linux: use the Docker engine that is already installed
        command -v docker > /dev/null || die "Install Docker (https://docs.docker.com/engine/install/) and try again."
        docker info > /dev/null 2>&1 || die "Docker is installed but not running, or you lack permission to use it."
        ensure_vscode
    fi
    info "Ready"
}

cmd_stop() {
    is_macos || { info "Nothing to stop: the Colima VM is only used on macOS"; return; }
    if vm_running; then
        info "Stopping Colima VM '$PROFILE'"
        colima stop "$PROFILE"
    else
        info "Colima VM '$PROFILE' is not running"
    fi
}

cmd_status() {
    if is_macos && command -v colima > /dev/null; then
        colima status "$PROFILE" 2>&1 || true
    else
        docker info --format 'Docker {{.ServerVersion}} is running' 2>&1 || true
    fi
}

cmd_docker_context() {
    if is_macos; then echo "$CONTEXT"; fi
}

usage() {
    sed -n '3,18p' "${BASH_SOURCE[0]}" | sed 's/^# \{0,1\}//'
}

main() {
    local command=""
    for arg in "$@"; do
        case "$arg" in
            --yes|-y) ASSUME_YES=1 ;;
            start|stop|status|code-path|docker-context) command=$arg ;;
            -h|--help) usage; exit 0 ;;
            *) die "Unknown argument: $arg (see --help)" ;;
        esac
    done
    case "$command" in
        start) cmd_start ;;
        stop) cmd_stop ;;
        status) cmd_status ;;
        code-path) code_path || die "VS Code's 'code' command was not found." ;;
        docker-context) cmd_docker_context ;;
        *) usage; exit 1 ;;
    esac
}

main "$@"
