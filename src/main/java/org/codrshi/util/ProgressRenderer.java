package org.codrshi.util;

import java.nio.file.Path;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Live multi-file progress panel for indexing operations.
 *
 * Each tracked file moves through three colored states:
 *   RED    - chunks queued, waiting for LLM summary
 *   YELLOW - LLM summary generated, waiting for embedding to be stored
 *   GREEN  - embedding stored in vector DB (file is "done")
 *
 * Completed files scroll above the live panel as static checkmark lines,
 * while in-flight files keep updating in place at the bottom.
 */
public class ProgressRenderer {

    private static final ProgressRenderer INSTANCE = new ProgressRenderer();

    public static ProgressRenderer get() {
        return INSTANCE;
    }

    private enum State { PENDING_SUMMARY, PENDING_EMBEDDING, COMPLETED }

    private static class FileProgress {
        final int totalChunks;
        int summarizedChunks;
        int storedChunks;
        State state;
        final long startNanos;

        FileProgress(int totalChunks) {
            this.totalChunks = Math.max(1, totalChunks);
            this.state = State.PENDING_SUMMARY;
            this.startNanos = System.nanoTime();
        }
    }

    private final LinkedHashMap<String, FileProgress> files = new LinkedHashMap<>();
    private int lastRenderedLines = 0;
    private int totalCompleted = 0;
    private int totalRemoved = 0;
    private boolean active = false;
    private long startNanos;

    public synchronized void begin(String title) {
        active = true;
        files.clear();
        lastRenderedLines = 0;
        totalCompleted = 0;
        totalRemoved = 0;
        startNanos = System.nanoTime();

        TerminalRenderer.hideCursor();
        TerminalRenderer.println();
        TerminalRenderer.println("  %s", TerminalRenderer.bold(title));
        TerminalRenderer.println("  %s", TerminalRenderer.dim("─".repeat(60)));
    }

    public synchronized void register(Path relativePath, int chunkCount) {
        if(!active) return;
        files.put(relativePath.toString(), new FileProgress(chunkCount));
        render();
    }

    public synchronized void markSummarized(Path relativePath) {
        markSummarized(relativePath.toString());
    }

    public synchronized void markSummarized(String filePath) {
        if(!active) return;
        FileProgress p = files.get(filePath);
        if(p == null) return;
        p.summarizedChunks++;
        if(p.summarizedChunks >= p.totalChunks && p.state == State.PENDING_SUMMARY) {
            p.state = State.PENDING_EMBEDDING;
        }
        render();
    }

    public synchronized void markStored(Path relativePath) {
        markStored(relativePath.toString());
    }

    public synchronized void markStored(String filePath) {
        if(!active) return;
        FileProgress p = files.get(filePath);
        if(p == null) return;
        p.storedChunks++;
        if(p.storedChunks >= p.totalChunks) {
            p.state = State.COMPLETED;
        }
        render();
    }

    public synchronized void noteRemoval(String filePath) {
        if(!active) {
            TerminalRenderer.println("  %s %s %s",
                    TerminalRenderer.gray("⊖"),
                    truncate(filePath, 55),
                    TerminalRenderer.dim("(removed)"));
            return;
        }
        clearPanel();
        TerminalRenderer.println("  %s %s %s",
                TerminalRenderer.gray("⊖"),
                truncate(filePath, 55),
                TerminalRenderer.dim("(removed)"));
        totalRemoved++;
        render();
    }

    public synchronized void end() {
        if(!active) return;

        clearPanel();
        flushCompleted();

        if(!files.isEmpty()) {
            for(Map.Entry<String, FileProgress> e : files.entrySet()) {
                TerminalRenderer.println("  %s %s %s",
                        TerminalRenderer.red("✗"),
                        truncate(e.getKey(), 55),
                        TerminalRenderer.dim("(incomplete)"));
            }
            files.clear();
        }

        long elapsedMs = (System.nanoTime() - startNanos) / 1_000_000;
        TerminalRenderer.println("  %s", TerminalRenderer.dim("─".repeat(60)));

        StringBuilder summary = new StringBuilder("  ");
        summary.append(TerminalRenderer.green("✔ "));
        summary.append(totalCompleted).append(" indexed");
        if(totalRemoved > 0) {
            summary.append(TerminalRenderer.dim(", ")).append(totalRemoved).append(" removed");
        }
        summary.append(TerminalRenderer.dim(String.format("  ·  %.1fs", elapsedMs / 1000.0)));
        TerminalRenderer.println("%s", summary.toString());
        TerminalRenderer.println();

        TerminalRenderer.showCursor();
        active = false;
    }

    public synchronized void abort() {
        if(!active) return;
        clearPanel();
        TerminalRenderer.showCursor();
        active = false;
    }

    private void render() {
        clearPanel();
        flushCompleted();

        StringBuilder sb = new StringBuilder();
        int lines = 0;
        for(Map.Entry<String, FileProgress> e : files.entrySet()) {
            FileProgress p = e.getValue();
            String color;
            String label;
            switch(p.state) {
                case PENDING_SUMMARY:
                    color = TerminalRenderer.RED;
                    label = "summarizing";
                    break;
                case PENDING_EMBEDDING:
                    color = TerminalRenderer.YELLOW;
                    label = "embedding  ";
                    break;
                default:
                    continue;
            }
            sb.append("  ")
              .append(TerminalRenderer.color(color, "● "))
              .append(TerminalRenderer.dim("[" + label + "]"))
              .append("  ")
              .append(truncate(e.getKey(), 55))
              .append('\n');
            lines++;
        }
        TerminalRenderer.printRaw(sb.toString());
        lastRenderedLines = lines;
    }

    private void flushCompleted() {
        StringBuilder sb = new StringBuilder();
        Iterator<Map.Entry<String, FileProgress>> iter = files.entrySet().iterator();
        while(iter.hasNext()) {
            Map.Entry<String, FileProgress> e = iter.next();
            if(e.getValue().state == State.COMPLETED) {
                double secs = (System.nanoTime() - e.getValue().startNanos) / 1e9;
                sb.append("  ")
                  .append(TerminalRenderer.green("✓ "))
                  .append(truncate(e.getKey(), 55))
                  .append(TerminalRenderer.dim(String.format("  (%.1fs)", secs)))
                  .append('\n');
                iter.remove();
                totalCompleted++;
            }
        }
        if(sb.length() > 0) {
            TerminalRenderer.printRaw(sb.toString());
        }
    }

    private void clearPanel() {
        if(lastRenderedLines == 0) return;
        TerminalRenderer.moveCursorUp(lastRenderedLines);
        TerminalRenderer.clearBelow();
        lastRenderedLines = 0;
    }

    private static String truncate(String s, int max) {
        if(s.length() <= max) return s;
        return "…" + s.substring(s.length() - max + 1);
    }
}
