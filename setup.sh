#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# AuRUS environment setup / doctor script.
#
# Detects what's installed, what's missing, and — where safe — installs it.
# Run from the repository root:
#
#   ./setup.sh            # check everything, print a report
#   ./setup.sh --install  # also attempt to install missing tools (apt/brew)
#   ./setup.sh --docker   # additionally build the bundled Strix Docker image
#   ./setup.sh --smoke    # end with a 1-generation smoke test on the arbiter
#
# Safe to re-run any time; every step is idempotent and non-destructive.
# ---------------------------------------------------------------------------
set -u

DO_INSTALL=0
DO_DOCKER=0
DO_SMOKE=0
for arg in "$@"; do
  case "$arg" in
    --install) DO_INSTALL=1 ;;
    --docker)  DO_DOCKER=1 ;;
    --smoke)   DO_SMOKE=1 ;;
    -h|--help)
      sed -n '2,14p' "$0"
      exit 0
      ;;
  esac
done

# ---- output helpers --------------------------------------------------------
OK="[  OK  ]"
MISSING="[ MISS ]"
WARN="[ WARN ]"
FIX="[ FIX  ]"
FAIL_COUNT=0
WARN_COUNT=0

pass() { printf '%s %s\n' "$OK" "$1"; }
fail() { printf '%s %s\n' "$MISSING" "$1"; FAIL_COUNT=$((FAIL_COUNT + 1)); }
warn() { printf '%s %s\n' "$WARN" "$1"; WARN_COUNT=$((WARN_COUNT + 1)); }
fix()  { printf '%s %s\n' "$FIX" "$1"; }
section() { printf '\n== %s ==\n' "$1"; }

# ---- OS / arch detection ----------------------------------------------------
OS="unknown"
ARCH="$(uname -m 2>/dev/null || echo unknown)"
case "$(uname -s 2>/dev/null)" in
  Linux)  OS="linux" ;;
  Darwin) OS="macos" ;;
  *)      OS="unknown" ;;
esac

section "System"
echo "OS:   $OS"
echo "Arch: $ARCH"
if [ "$OS" = "macos" ] && [ "$ARCH" = "arm64" ]; then
  warn "Apple Silicon detected. AuRUS's vendored native tools are x86_64-only;"
  echo "         they will run under Rosetta 2 if it is installed, or fail otherwise."
  echo "         Docker is the more reliable path here (Docker Desktop emulates x86_64)."
fi

# ---- package manager for --install -----------------------------------------
PKG_INSTALL=""
if [ "$DO_INSTALL" -eq 1 ]; then
  if [ "$OS" = "macos" ] && command -v brew >/dev/null 2>&1; then
    PKG_INSTALL="brew install"
  elif [ "$OS" = "linux" ] && command -v apt-get >/dev/null 2>&1; then
    PKG_INSTALL="sudo apt-get install -y"
  elif [ "$OS" = "linux" ] && command -v dnf >/dev/null 2>&1; then
    PKG_INSTALL="sudo dnf install -y"
  else
    warn "--install requested but no supported package manager found (brew/apt/dnf)."
    echo "         You'll need to install missing tools manually; see the messages below."
  fi
fi

try_install() {
  # try_install <human name> <package name>
  if [ -n "$PKG_INSTALL" ]; then
    fix "Installing $1 ($PKG_INSTALL $2)..."
    eval "$PKG_INSTALL $2"
  else
    echo "         Install manually: $1"
  fi
}

# ---- Java -------------------------------------------------------------------
section "Java (>= 11 required)"
if command -v java >/dev/null 2>&1; then
  JAVA_VER_RAW="$(java -version 2>&1 | head -1)"
  JAVA_MAJOR="$(java -version 2>&1 | head -1 | sed -E 's/.*"([0-9]+)\..*/\1/; s/.*"([0-9]+)"/\1/')"
  if [ "${JAVA_MAJOR:-0}" -ge 11 ] 2>/dev/null; then
    pass "java found: $JAVA_VER_RAW"
  else
    fail "java found but too old: $JAVA_VER_RAW (need >= 11)"
    [ "$DO_INSTALL" -eq 1 ] && try_install "OpenJDK 11+" "openjdk-11-jdk"
  fi
else
  fail "java not found on PATH"
  [ "$DO_INSTALL" -eq 1 ] && try_install "OpenJDK 11+" "openjdk-11-jdk"
fi

# ---- Ant ----------------------------------------------------------------
section "Apache Ant"
if command -v ant >/dev/null 2>&1; then
  pass "ant found: $(ant -version 2>&1 | head -1)"
else
  fail "ant not found on PATH"
  [ "$DO_INSTALL" -eq 1 ] && try_install "Apache Ant" "ant"
fi

# ---- Docker -------------------------------------------------------------
section "Docker (needed for -docker, and effectively required on Linux — see below)"
if command -v docker >/dev/null 2>&1; then
  if docker info >/dev/null 2>&1; then
    pass "docker found and daemon is running: $(docker --version)"
    DOCKER_OK=1
  else
    warn "docker is installed but the daemon does not seem to be running"
    echo "         Start Docker Desktop (macOS) or the docker service (Linux) and re-run."
    DOCKER_OK=0
  fi
else
  fail "docker not found on PATH"
  DOCKER_OK=0
  if [ "$DO_INSTALL" -eq 1 ]; then
    if [ "$OS" = "macos" ]; then
      echo "         Docker Desktop can't be silently installed by this script on macOS."
      echo "         Download it from https://www.docker.com/products/docker-desktop/"
    else
      try_install "Docker Engine" "docker.io"
    fi
  fi
fi

# ---- vendored native tools in lib/ ---------------------------------------
section "Vendored tools in lib/ (syfco, aalta, Strix)"

chmod +x lib/syfco lib/syfco_macos lib/aalta lib/aalta_linux lib/pltl \
         lib/new_strix/strix lib/strix_tlsf.sh 2>/dev/null

# syfco: OS-aware selection already exists in TlsfUtils.getCommand(); just
# confirm the right binary for this OS actually runs.
SYFCO_BIN="lib/syfco"
[ "$OS" = "macos" ] && SYFCO_BIN="lib/syfco_macos"
if [ -x "$SYFCO_BIN" ]; then
  if "./$SYFCO_BIN" --version >/dev/null 2>&1 || "./$SYFCO_BIN" -h >/dev/null 2>&1; then
    pass "syfco ($SYFCO_BIN) runs correctly"
  else
    fail "syfco ($SYFCO_BIN) is present but did not run (check $(file "$SYFCO_BIN" 2>/dev/null))"
  fi
else
  fail "$SYFCO_BIN not found or not executable"
fi

# aalta: LTLSolver.java already branches on OS for this one; just sanity-check.
AALTA_BIN="lib/aalta_linux"
[ "$OS" = "macos" ] && AALTA_BIN="lib/aalta"
if [ -x "$AALTA_BIN" ]; then
  pass "aalta ($AALTA_BIN) present and executable"
else
  fail "$AALTA_BIN not found or not executable"
fi

# Strix native binary: THE KNOWN GAP. lib/new_strix/strix is hardcoded in
# StrixHelper with no OS check, and the binary currently committed is
# macOS-only (Mach-O). On Linux this fails with "Exec format error", which
# is swallowed by the fitness function's try/catch — every candidate then
# silently scores 0 and no repair is ever found, with no visible error.
echo ""
echo "  Checking lib/new_strix/strix (the binary StrixHelper calls when"
echo "  -docker is NOT passed) against this OS..."
STRIX_FILE_INFO="$(file lib/new_strix/strix 2>/dev/null)"
STRIX_NATIVE_OK=0
case "$OS" in
  linux)
    if echo "$STRIX_FILE_INFO" | grep -q "ELF"; then
      STRIX_NATIVE_OK=1
    fi
    ;;
  macos)
    if echo "$STRIX_FILE_INFO" | grep -q "Mach-O"; then
      STRIX_NATIVE_OK=1
    fi
    ;;
esac

if [ "$STRIX_NATIVE_OK" -eq 1 ]; then
  pass "lib/new_strix/strix matches this OS ($STRIX_FILE_INFO)"
else
  warn "lib/new_strix/strix does NOT match this OS: $STRIX_FILE_INFO"
  echo "         >>> Known limitation: the committed native Strix binary is macOS-only."
  echo "         >>> On $OS, running WITHOUT -docker will silently find zero repairs"
  echo "         >>> (the realizability check fails for every candidate; the failure"
  echo "         >>> is caught internally and never surfaced as an error)."
  echo "         >>> Fix: always pass -docker on this machine, or build/vendor a"
  echo "         >>> native Strix binary for $OS/$ARCH at lib/new_strix/strix."
fi

# ---- Strix via Docker -----------------------------------------------------
section "Strix Docker image"
if [ "${DOCKER_OK:-0}" -eq 1 ]; then
  if docker image inspect strix_image >/dev/null 2>&1; then
    pass "Docker image 'strix_image' already built"
  else
    if [ "$DO_DOCKER" -eq 1 ]; then
      fix "Building the Strix Docker image (cd lib && docker build -t strix_image .)..."
      (cd lib && docker build -t strix_image .)
    else
      warn "Docker image 'strix_image' not built yet"
      echo "         Build it with: cd lib && docker build -t strix_image ."
      echo "         (or re-run this script with --docker to build it automatically)"
    fi
  fi
else
  warn "Docker not available — skipping image check (see Docker section above)"
fi

# ---- summary ---------------------------------------------------------------
section "Summary"
if [ "$FAIL_COUNT" -eq 0 ] && [ "$WARN_COUNT" -eq 0 ]; then
  echo "Everything looks good."
elif [ "$FAIL_COUNT" -eq 0 ]; then
  echo "$WARN_COUNT warning(s) above — AuRUS should run, but read them."
else
  echo "$FAIL_COUNT missing item(s) and $WARN_COUNT warning(s) — fix the [ MISS ] lines"
  echo "above before running AuRUS (re-run with --install to attempt automatic fixes)."
fi

if [ "$OS" = "linux" ] && [ "$STRIX_NATIVE_OK" -eq 0 ]; then
  echo ""
  echo "Recommended command on this machine: always add -docker, e.g."
  echo "  ./unreal-repair.sh -docker case-studies/arbiter/arbiter.tlsf"
fi

# ---- optional smoke test ----------------------------------------------------
if [ "$DO_SMOKE" -eq 1 ]; then
  section "Smoke test (1 generation on the arbiter case study)"
  EXTRA_FLAG=""
  [ "$OS" = "linux" ] && [ "$STRIX_NATIVE_OK" -eq 0 ] && EXTRA_FLAG="-docker"
  if [ ! -d bin ] || [ -z "$(find bin -name '*.class' -print -quit 2>/dev/null)" ]; then
    fix "Compiling first (ant compile)..."
    ant compile >/dev/null 2>&1
  fi
  ./unreal-repair.sh -Gen=1 -Max=5 $EXTRA_FLAG case-studies/arbiter/arbiter.tlsf
  echo ""
  echo "If you saw 'Realizable Specifications:' with at least one entry above,"
  echo "the environment is correctly set up end to end."
fi

exit 0