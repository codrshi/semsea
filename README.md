# semsea

Semantic codebase search powered by a local LLM and vector store. Index a
workspace once, then locate files in it by **describing what you want**
instead of remembering names.

```text
$ semsea find "where the database connection url is built"

  Query: "where the database connection url is built"
  Scope: my-project

   #  File                                                          Last Modified
  --  ------------------------------------------------------------  ------------------------
   1  src/main/java/org/codrshi/repository/DbManager.java           2026-06-09 09:18:42
   2  src/main/resources/semsea.default.json                        2026-06-04 12:01:11
   3  ...
```

## Requirements

semsea talks to three local services. Get them running before using the CLI:

| Service       | Role                              | Default URL                |
|---------------|-----------------------------------|----------------------------|
| Ollama        | Embedding model + summariser      | `http://localhost:11434`   |
| ChromaDB      | Vector store                      | `http://localhost:8000`    |
| Java 21+      | Runtime                           | -                          |

Detailed step-by-step setup (Docker, GPU detection, model sizing per
hardware tier, Apple Silicon native path, troubleshooting) is in
[SERVICES.md](SERVICES.md). After bringing the services up, run
`semsea heartbeat` to verify connectivity end-to-end.

## Install

### Windows

1. Download `semsea-<version>.zip` from a release.
2. Extract it anywhere.
3. From the extracted directory, run PowerShell and:
   ```powershell
   .\install.ps1
   ```
4. Restart your shell. Verify:
   ```powershell
   semsea --help
   ```

### macOS / Linux

1. Download `semsea-<version>.tar.gz` from a release.
2. Extract and install:
   ```sh
   tar -xzf semsea-<version>.tar.gz
   cd semsea-<version>
   ./install.sh
   ```
3. Open a new shell (or `source` your shell rc). Verify:
   ```sh
   semsea --help
   ```

The installer drops the JAR + launcher under a stable location and puts
`semsea` on your PATH. It never copies config or database files into the
install dir — those go to your per-user data directory the first time you
run `semsea`.

## Where semsea keeps its data

| OS      | Path                                                       |
|---------|------------------------------------------------------------|
| Windows | `%APPDATA%\semsea\`                                        |
| macOS   | `~/Library/Application Support/semsea/`                    |
| Linux   | `$XDG_CONFIG_HOME/semsea/` (default `~/.config/semsea/`)   |

Layout under that directory:
```
semsea/
  semsea.json         # editable settings (indexing rules, batch sizes)
  .db/semsea.db       # SQLite index metadata
  logs/app-output.log # rolling application log
```

Override the location by setting `SEMSEA_HOME=/some/path` before launching.

## Quick start

```sh
# 1. Index a workspace
semsea attach my-project --path ./path/to/code

# 2. Find files
semsea find "the cli command that switches workspaces"

# 3. Re-index after changes
semsea refresh

# 4. Manage multiple workspaces
semsea list
semsea switch other-project
semsea remove old-project
```

Run `semsea --help` for the full command set and `semsea <command> --help`
for command-specific options.

## Uninstall

Delete the install dir the installer reported, remove its `bin/` entry from
your PATH, and (optionally) delete your data dir from the table above.

## Building from source

```sh
mvn package
```

Produces:
- `target/semsea.jar`                  - executable uber-JAR
- `target/semsea-<version>.zip`        - Windows distribution
- `target/semsea-<version>.tar.gz`     - Unix distribution

## License

MIT - see [LICENSE](LICENSE).
