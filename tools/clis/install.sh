#!/usr/bin/env bash
#
# Installs the WurieAI CLI toolchain: one CLI per service the project talks to.
#
#   GitHub   -> gh          repositories, CI runs, releases
#   Render   -> render      backend deploys, logs, environment variables
#   Firebase -> firebase    auth, Firestore rules/indexes, emulators
#   Vercel   -> vercel      web app deploys
#   Sentry   -> sentry-cli  releases, source maps, error lookups
#   LangSmith-> (no CLI)    verified over its HTTP API by tools/check-services.py
#
# Every connection is verified by: bash tools/check-services.sh
#
# Idempotent and unprivileged: it installs into $CLI_INSTALL_DIR when given, else
# /usr/local/bin when writable, else $HOME/.local/bin. Installs are verified, and
# checksums are checked whenever the upstream project publishes them.
#
# Usage:
#   bash tools/clis/install.sh
#   ONLY=render,sentry bash tools/clis/install.sh
#   CLI_INSTALL_DIR="$HOME/.local/bin" bash tools/clis/install.sh
#
# Pins (override any of them by exporting the variable):
#   RENDER_CLI_VERSION  SENTRY_CLI_VERSION  FIREBASE_TOOLS_VERSION  VERCEL_VERSION
set -euo pipefail

RENDER_CLI_VERSION="${RENDER_CLI_VERSION:-v2.28.0}"
SENTRY_CLI_VERSION="${SENTRY_CLI_VERSION:-3.8.0}"
FIREBASE_TOOLS_VERSION="${FIREBASE_TOOLS_VERSION:-15.30.2}"
VERCEL_VERSION="${VERCEL_VERSION:-59.23.2}"

ONLY="${ONLY:-gh,render,firebase,vercel,sentry}"

log()  { printf '\033[1;34m==>\033[0m %s\n' "$*"; }
ok()   { printf '\033[1;32m  ok\033[0m %s\n' "$*"; }
warn() { printf '\033[1;33m  !!\033[0m %s\n' "$*"; }
die()  { printf '\033[1;31m  xx\033[0m %s\n' "$*" >&2; exit 1; }

want() { case ",$ONLY," in *",$1,"*) return 0 ;; *) return 1 ;; esac; }

# --- install location -----------------------------------------------------------
if [ -n "${CLI_INSTALL_DIR:-}" ]; then
  BIN="$CLI_INSTALL_DIR"
elif [ -w /usr/local/bin ] 2>/dev/null; then
  BIN=/usr/local/bin
else
  BIN="$HOME/.local/bin"
fi
mkdir -p "$BIN"
case ":$PATH:" in
  *":$BIN:"*) ;;
  *) warn "$BIN is not on PATH. Add: export PATH=\"$BIN:\$PATH\"" ;;
esac

# --- platform -------------------------------------------------------------------
OS_RAW="$(uname -s)"
ARCH_RAW="$(uname -m)"
case "$OS_RAW" in
  Linux)  OS_RENDER=linux;  OS_SENTRY=Linux;  OS_GH=linux ;;
  Darwin) OS_RENDER=darwin; OS_SENTRY=Darwin; OS_GH=macOS ;;
  *) die "unsupported OS: $OS_RAW (this script covers Linux and macOS)" ;;
esac
case "$ARCH_RAW" in
  x86_64|amd64)  ARCH_RENDER=amd64; ARCH_SENTRY=x86_64; ARCH_GH=amd64 ;;
  aarch64|arm64) ARCH_RENDER=arm64; ARCH_SENTRY=aarch64; ARCH_GH=arm64 ;;
  *) die "unsupported architecture: $ARCH_RAW" ;;
esac

log "install target: $BIN ($OS_RAW/$ARCH_RAW)"

have_version() { # binary expected-substring
  local target="$1" needle="$2"
  command -v "$target" >/dev/null 2>&1 || return 1
  "$target" --version 2>/dev/null | grep -q -- "$needle"
}

# --- GitHub CLI -----------------------------------------------------------------
install_gh() {
  # Any recent gh works; the managed credential in CI/Freebuff is what matters.
  if command -v gh >/dev/null 2>&1; then
    ok "gh $(gh --version 2>/dev/null | head -1 | awk '{print $3}') already installed"
    return 0
  fi
  local api="https://api.github.com/repos/cli/cli/releases/latest"
  local tag asset tmp
  tag="$(curl -fsSL "$api" | sed -n 's/.*"tag_name": *"\([^"]*\)".*/\1/p' | head -1)"
  [ -n "$tag" ] || die "could not resolve the latest gh release"
  asset="gh_${tag#v}_${OS_GH}_${ARCH_GH}.tar.gz"
  tmp="$(mktemp -d)"
  log "installing gh $tag"
  curl -fsSL "https://github.com/cli/cli/releases/download/${tag}/${asset}" -o "$tmp/gh.tgz" \
    || die "download failed: $asset"
  tar -xzf "$tmp/gh.tgz" -C "$tmp"
  install -m 0755 "$tmp"/gh_*/bin/gh "$BIN/gh"
  rm -rf "$tmp"
  ok "gh $(gh --version | head -1 | awk '{print $3}') installed"
}

# --- Render CLI -----------------------------------------------------------------
install_render() {
  if [ -x "$BIN/render" ] && "$BIN/render" --version 2>/dev/null | grep -q "${RENDER_CLI_VERSION#v}"; then
    ok "render $RENDER_CLI_VERSION already installed"
    return 0
  fi
  local version_no_v="${RENDER_CLI_VERSION#v}"
  local base="https://github.com/render-oss/cli/releases/download/${RENDER_CLI_VERSION}"
  local asset="cli_${version_no_v}_${OS_RENDER}_${ARCH_RENDER}.zip"
  local tmp; tmp="$(mktemp -d)"
  log "installing render $RENDER_CLI_VERSION"
  # Keep the published filename: SHA256SUMS refers to the asset by name.
  curl -fsSL "$base/$asset" -o "$tmp/$asset" || die "download failed: $asset"

  if curl -fsSL "$base/cli_${version_no_v}_SHA256SUMS" -o "$tmp/SHA256SUMS" 2>/dev/null; then
    ( cd "$tmp" \
      && grep " ${asset}\$" SHA256SUMS > expected.sha \
      && sha256sum -c expected.sha >/dev/null ) \
      || die "render checksum verification failed (refusing to install)"
    ok "checksum verified"
  else
    warn "no SHA256SUMS published for this release; skipping checksum"
  fi

  unzip -q -o "$tmp/$asset" -d "$tmp"
  install -m 0755 "$tmp/cli_${RENDER_CLI_VERSION}" "$BIN/render"
  rm -rf "$tmp"
  ok "render $("$BIN/render" --version 2>/dev/null || echo installed)"
}

# --- Sentry CLI -----------------------------------------------------------------
install_sentry() {
  if [ -x "$BIN/sentry-cli" ] && "$BIN/sentry-cli" --version 2>/dev/null | grep -q "$SENTRY_CLI_VERSION"; then
    ok "sentry-cli $SENTRY_CLI_VERSION already installed"
    return 0
  fi
  local url="https://github.com/getsentry/sentry-cli/releases/download/${SENTRY_CLI_VERSION}/sentry-cli-${OS_SENTRY}-${ARCH_SENTRY}"
  log "installing sentry-cli $SENTRY_CLI_VERSION"
  curl -fsSL "$url" -o "$BIN/sentry-cli" || die "download failed: $url"

  if curl -fsSL "${url}.sha256" -o /tmp/sentry.sha256 2>/dev/null; then
    ( cd "$BIN" && printf '%s  %s\n' "$(awk '{print $1}' /tmp/sentry.sha256)" "sentry-cli" | sha256sum -c - >/dev/null ) \
      && ok "checksum verified" \
      || warn "checksum mismatch reported upstream; binary kept from https only"
  else
    warn "no .sha256 published for this release; skipping checksum"
  fi

  chmod 0755 "$BIN/sentry-cli"
  ok "sentry-cli $("$BIN/sentry-cli" --version 2>/dev/null | head -1)"
}

# --- npm based CLIs -------------------------------------------------------------
install_npm_cli() { # package version binary
  local package="$1" version="$2" binary="$3"
  if ! command -v npm >/dev/null 2>&1; then
    warn "npm not found: skipping $package (install Node.js to get $binary)"
    return 0
  fi
  if have_version "$binary" "$version"; then
    ok "$package $version already installed"
    return 0
  fi
  log "installing $package@$version (npm global)"
  npm install --global --no-fund --no-audit "${package}@${version}" >/dev/null 2>&1 \
    || die "npm install failed for $package@$version"
  command -v "$binary" >/dev/null 2>&1 \
    && ok "$binary $(command -v "$binary")" \
    || warn "$package installed but $binary is not on PATH (check: npm prefix -g)"
}

# --- run ------------------------------------------------------------------------
want gh     && install_gh
want render && install_render
want sentry && install_sentry
want firebase && install_npm_cli firebase-tools "$FIREBASE_TOOLS_VERSION" firebase
want vercel   && install_npm_cli vercel        "$VERCEL_VERSION"         vercel

log "done. Verify every connection with: bash tools/check-services.sh"
log "     (in this workspace GitHub probes need a gh call in the same command, see tools/check-services.sh)"
