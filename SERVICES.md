# Backing Services Setup

`semsea` is a thin client over three local services. You need all three running
before any `semsea` command except `--help` will do useful work.

| Service     | Role                                             | Default URL                |
|-------------|--------------------------------------------------|----------------------------|
| Ollama      | Hosts the LLM (summarisation) and embedding model| `http://localhost:11434`   |
| ChromaDB    | Vector store for the embedded summaries          | `http://localhost:8000`    |
| Docker      | Container runtime that hosts both above          | -                          |

This guide walks you through bringing them up. After install you can verify
everything end-to-end with:

```sh
semsea heartbeat
```

---

## 0. Prerequisites

### 0.1 Install Docker

| OS              | Recommended                                                              |
|-----------------|--------------------------------------------------------------------------|
| Windows 10/11   | [Docker Desktop](https://docs.docker.com/desktop/install/windows-install/) (uses WSL2) |
| macOS           | [Docker Desktop](https://docs.docker.com/desktop/install/mac-install/) (Intel or Apple Silicon) |
| Linux           | [Docker Engine](https://docs.docker.com/engine/install/) (avoid Docker Desktop unless you need GUI) |

Verify:

```sh
docker --version
docker run --rm hello-world
```

### 0.2 Detect your GPU

Ollama auto-uses a GPU if it can see one. Knowing what you have decides
**which Ollama image flag** to pass and **which model size** to pull.

#### Windows

PowerShell:

```powershell
Get-CimInstance Win32_VideoController | Select-Object Name, AdapterRAM
```

If you see an NVIDIA GPU and want Docker to use it, you must be on
**Windows + WSL2** with the NVIDIA driver installed on Windows (not inside
WSL). Docker Desktop picks it up automatically.

#### macOS

```sh
system_profiler SPDisplaysDataType | grep -E "Chipset Model|VRAM"
```

- **Apple Silicon (M-series)**: no discrete GPU is needed. The Ollama
  **native macOS app** uses the Apple Metal API and is faster than the
  Docker version. For Apple Silicon, **use the native app** (see §1b).
- **Intel Mac with no GPU**: CPU-only, follow §1c.

#### Linux

```sh
nvidia-smi                # NVIDIA users - prints driver + GPU info
# or
lspci | grep -E "VGA|3D"
```

To pass the GPU into Docker on Linux you also need the
[NVIDIA Container Toolkit](https://docs.nvidia.com/datacenter/cloud-native/container-toolkit/latest/install-guide.html):

```sh
# Debian/Ubuntu (one-time)
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg
curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | \
  sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
  sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list
sudo apt-get update && sudo apt-get install -y nvidia-container-toolkit
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker

# Verify
docker run --rm --gpus all nvidia/cuda:12.2.0-base-ubuntu22.04 nvidia-smi
```

### 0.3 Pick model sizes for your hardware

Ollama hosts two models for `semsea`:

- **Embedding model**: `nomic-embed-text` (~275 MB). Lightweight, runs fine on
  any modern machine. Use it as-is.
- **LLM (summariser)**: pick a `qwen2.5-coder` variant based on VRAM. `semsea`
  reads the model name from your `semsea.json`'s `llmContextLimit` /
  internal config; the default expects `qwen2.5-coder:3b`.

| Setup                                | Suggested LLM              | Notes                                              |
|--------------------------------------|----------------------------|----------------------------------------------------|
| NVIDIA GPU, 8 GB+ VRAM               | `qwen2.5-coder:7b`         | Best summary quality. Update config (§5).          |
| NVIDIA GPU, 4-6 GB VRAM              | `qwen2.5-coder:3b`         | Default. Good balance of quality vs. speed.        |
| NVIDIA GPU, < 4 GB VRAM              | `qwen2.5-coder:1.5b`       | Lower quality, but fits. Update config.            |
| Apple Silicon (M1/M2/M3, 8 GB RAM)   | `qwen2.5-coder:3b`         | Use the native Ollama app, not Docker.             |
| Apple Silicon (M1/M2/M3, 16 GB+ RAM) | `qwen2.5-coder:7b`         | Update config.                                     |
| CPU-only (any OS)                    | `qwen2.5-coder:1.5b`       | Indexing will be slow but functional.              |

Rule of thumb: an LLM needs roughly **1.5x its parameter count in GB** of
free VRAM (or system RAM in CPU mode). 3B ≈ 4-5 GB, 7B ≈ 8-12 GB.

---

## 1. Ollama

Pick **one** of the following paths.

### 1a. Linux/Windows with NVIDIA GPU (Docker, recommended)

```sh
docker run -d --gpus all --name ollama -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  ollama/ollama
```

Verify GPU is being used:

```sh
docker exec ollama nvidia-smi
```

### 1b. macOS Apple Silicon (native app, recommended)

Docker on Apple Silicon **cannot pass through the Apple GPU**. Use the
native build, which uses Metal and is several times faster than CPU
Docker:

1. Download from [ollama.com/download/mac](https://ollama.com/download/mac).
2. Run the installer.
3. The app starts a server on `http://localhost:11434` automatically.

If you prefer Docker anyway, you'll fall back to CPU mode (§1c).

### 1c. CPU-only (any OS, Docker)

```sh
docker run -d --name ollama -p 11434:11434 \
  -v ollama_data:/root/.ollama \
  ollama/ollama
```

Identical command, just **without** `--gpus all`. Expect summarisation to
be 5-20x slower depending on your CPU. Use a smaller model (§0.3).

### 1d. Pull the models

Once the server is up, pull the models you chose in §0.3:

```sh
# Required: embedding model
ollama pull nomic-embed-text

# Required: at least one LLM (pick what fits your hardware)
ollama pull qwen2.5-coder:3b      # default
# or
ollama pull qwen2.5-coder:7b
# or
ollama pull qwen2.5-coder:1.5b
```

If you're running Ollama in Docker, prefix with `docker exec -it ollama`:

```sh
docker exec -it ollama ollama pull nomic-embed-text
docker exec -it ollama ollama pull qwen2.5-coder:3b
```

### 1e. Verify Ollama

```sh
curl http://localhost:11434/api/tags
```

You should see both `nomic-embed-text` and a `qwen2.5-coder:*` entry in
the response.

---

## 2. ChromaDB

ChromaDB does **not** benefit from a GPU — it's a vector index that runs
on CPU. Same command on every platform:

```sh
docker run -d --name chromadb -p 8000:8000 \
  -v chroma-data:/chroma \
  chromadb/chroma:1.5.9
```

Verify:

```sh
curl http://localhost:8000/api/v2/heartbeat
# {"nanosecond heartbeat": <number>}
```

---

## 3. End-to-end verification

With Ollama and ChromaDB running, the CLI has its own health probe:

```sh
semsea heartbeat
```

You should see green ticks for SQLite, ChromaDB, and Ollama (including
the model checks). If any line is red, jump to §6 troubleshooting.

---

## 4. Day-to-day operations

```sh
# Stop both services (data persists in the docker volumes)
docker stop ollama chromadb

# Resume later
docker start ollama chromadb

# Remove a service entirely (data preserved in named volume)
docker rm -f ollama
docker volume ls    # ollama_data + chroma-data still listed

# Nuke everything including indexed data
docker rm -fv ollama chromadb
docker volume rm ollama_data chroma-data
```

---

## 5. Using a non-default LLM model

If you pulled something other than `qwen2.5-coder:3b` (e.g. `7b` for a
beefier GPU), tell `semsea` about it. The model id used during
summarisation is configurable but currently lives in code — open an
issue or PR to expose it via `semsea config` if you need it changed.

> *(If you're contributing: the model id is referenced in
> `org.codrshi.api.LLMClient`. A future `semsea config set --llm-model
> qwen2.5-coder:7b` will plumb this through cleanly.)*

The embedding model name (`nomic-embed-text`) is fixed across hardware
profiles; do not change it without also re-indexing every workspace.

---

## 6. Troubleshooting

### `semsea heartbeat` shows Ollama unreachable

- Container running? `docker ps | grep ollama`
- Port reachable? `curl http://localhost:11434/api/version`
- Firewall blocking 11434? Allow loopback traffic.
- On WSL2: did you reboot Docker Desktop after enabling GPU support?

### `semsea heartbeat` shows ChromaDB unreachable

- `docker ps | grep chromadb` — is it up?
- `docker logs chromadb` — any startup errors?
- Port 8000 in use by another app? `netstat -ano | findstr 8000` (win) /
  `lsof -i :8000` (mac/linux). Remap by changing `-p 8000:8000` to
  `-p 8800:8000` and update `chromaUrl` in `semsea.json`.

### Ollama OOM / "model requires more memory"

You picked a model that doesn't fit. Pull a smaller variant (§0.3) and
remove the oversized one:

```sh
ollama rm qwen2.5-coder:7b
ollama pull qwen2.5-coder:3b
```

### Indexing is unusably slow

- Confirm the GPU is actually being used: `docker exec ollama nvidia-smi`
  during a `semsea attach`. If GPU utilisation stays at 0 %, you're on CPU.
- On Apple Silicon: switch from Docker Ollama to the native app (§1b).
- Reduce the workspace surface area by tuning ignore rules:
  `semsea config set --add-ignored-dirs <dir>` or
  `--add-ignored-files <file>`.
- Drop to `qwen2.5-coder:1.5b` for huge codebases.

### `docker: Error response from daemon: could not select device driver "" with capabilities: [[gpu]]`

You skipped the NVIDIA Container Toolkit install on Linux (§0.2 Linux),
or you're trying `--gpus all` from a macOS / WSL2 setup that hasn't
exposed the GPU yet.

### Where do my embeddings live?

Inside the `chroma-data` Docker volume on disk. `docker volume inspect
chroma-data` shows the host path. `semsea`'s own metadata (workspace ids,
last refresh, etc.) is in the SQLite file under the per-user data
directory listed in `README.md`.
