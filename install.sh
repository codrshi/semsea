#!/usr/bin/env sh
#
# Installs the semsea CLI on macOS / Linux.
#
# Copies the launcher + JAR under $PREFIX (default ~/.local/share/semsea/bin)
# and creates a symlink under $BIN_DIR (default ~/.local/bin) so 'semsea' is
# available on PATH. The application's own data (config, database, logs) is
# created lazily on the first 'semsea' run under the OS-appropriate user data
# dir (~/Library/Application Support/semsea on macOS,
# $XDG_CONFIG_HOME/semsea or ~/.config/semsea on Linux).
#
# Environment overrides:
#   PREFIX           install location  (default: ~/.local/share/semsea)
#   BIN_DIR          symlink dir       (default: ~/.local/bin)

set -eu

PREFIX="${PREFIX:-$HOME/.local/share/semsea}"
BIN_DIR="${BIN_DIR:-$HOME/.local/bin}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
SOURCE_BIN="$SCRIPT_DIR/bin"

section() { printf "\n==> %s\n" "$1"; }

if [ ! -d "$SOURCE_BIN" ]; then
    echo "ERROR: 'bin/' not found next to this script. Did you extract the full archive?" >&2
    exit 1
fi

# 1. Java check
section "Checking Java"
if ! command -v java >/dev/null 2>&1; then
    echo "ERROR: Java is not on PATH. Install JDK 21 or newer and retry." >&2
    exit 1
fi
java -version 2>&1 | head -n 1 | sed 's/^/    /'

# 2. Copy files
section "Installing to $PREFIX"
TARGET_BIN="$PREFIX/bin"
mkdir -p "$TARGET_BIN"
cp -f "$SOURCE_BIN"/* "$TARGET_BIN/"
chmod +x "$TARGET_BIN/semsea"

# 3. Symlink onto PATH
section "Linking into $BIN_DIR"
mkdir -p "$BIN_DIR"
ln -sf "$TARGET_BIN/semsea" "$BIN_DIR/semsea"
echo "    Symlinked $BIN_DIR/semsea -> $TARGET_BIN/semsea"

# 4. PATH guidance
case ":$PATH:" in
    *":$BIN_DIR:"*)
        ;;
    *)
        section "PATH not configured"
        echo "    $BIN_DIR is NOT on your PATH."
        echo "    Add this line to your shell profile (~/.bashrc, ~/.zshrc, etc.):"
        echo ""
        echo "        export PATH=\"$BIN_DIR:\$PATH\""
        echo ""
        ;;
esac

# 5. Done
section "Done"
echo "    semsea installed to $PREFIX"
case "$(uname -s)" in
    Darwin) echo "    Data directory     ~/Library/Application Support/semsea (created on first run)" ;;
    Linux)  echo "    Data directory     \${XDG_CONFIG_HOME:-\$HOME/.config}/semsea (created on first run)" ;;
    *)      echo "    Data directory     created on first run by the application" ;;
esac
echo ""
echo "Open a new shell and try:"
echo "    semsea --help"
echo ""
echo "Before using 'semsea attach', bring up Ollama + ChromaDB."
echo "See SERVICES.md (next to this script) for step-by-step setup,"
echo "then verify with 'semsea heartbeat'."
