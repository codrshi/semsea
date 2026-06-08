package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.api.ChromaClient;
import org.codrshi.api.LLMClient;
import org.codrshi.api.OllamaClient;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbManager;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

@Command(
        name = "heartbeat",
        description = "Check connectivity to SQLite, ChromaDB, and Ollama.",
        mixinStandardHelpOptions = true
)
public class HeartbeatCommand implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(HeartbeatCommand.class);

    private static final int NAME_WIDTH   = 10;
    private static final int STATUS_WIDTH = 12;
    private static final int LATENCY_WIDTH = 8;

    private static final String SQLITE_HEARTBEAT_QUERY = "SELECT COUNT(*) FROM workspace;";

    private enum Status {
        OK("OK",       TerminalRenderer.GREEN,  "+"),
        DEGRADED("DEGRADED", TerminalRenderer.YELLOW, "!"),
        FAILED("FAILED", TerminalRenderer.RED,    "x");

        final String label;
        final String color;
        final String glyph;

        Status(String label, String color, String glyph) {
            this.label = label;
            this.color = color;
            this.glyph = glyph;
        }
    }

    private record CheckResult(String name, Status status, long elapsedMs, String detail) {}

    @Spec
    CommandSpec commandSpec;

    @Override
    public Integer call() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'heartbeat' invoked");

        List<CheckResult> results = new ArrayList<>();
        results.add(checkSQLite());
        results.add(checkChromaDB());
        results.add(checkOllama());

        renderReport(results);

        long healthy  = results.stream().filter(r -> r.status() == Status.OK).count();
        long degraded = results.stream().filter(r -> r.status() == Status.DEGRADED).count();
        long failed   = results.stream().filter(r -> r.status() == Status.FAILED).count();

        log.info("'heartbeat' completed: {}/{} healthy, {} degraded, {} failed",
                healthy, results.size(), degraded, failed);
        MetricCollector.print("HEARTBEAT_COMMAND");

        return failed > 0 ? 1 : 0;
    }

    private CheckResult checkSQLite() {
        long startNanos = System.nanoTime();
        try (
                Connection connection = DbManager.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(SQLITE_HEARTBEAT_QUERY)
        ) {
            int count = rs.next() ? rs.getInt(1) : 0;
            long elapsed = elapsedMs(startNanos);
            log.info("SQLite heartbeat OK ({} workspaces, {} ms)", count, elapsed);
            return new CheckResult("SQLite", Status.OK, elapsed,
                    count + " workspace row(s) accessible");
        }
        catch (Exception e) {
            long elapsed = elapsedMs(startNanos);
            log.error("SQLite heartbeat failed", e);
            return new CheckResult("SQLite", Status.FAILED, elapsed,
                    "could not query local database");
        }
    }

    private CheckResult checkChromaDB() {
        long startNanos = System.nanoTime();
        try {
            new ChromaClient().heartbeat();
            long elapsed = elapsedMs(startNanos);
            log.info("ChromaDB heartbeat OK ({} ms)", elapsed);
            return new CheckResult("ChromaDB", Status.OK, elapsed, ChromaClient.BASE_URL);
        }
        catch (Exception e) {
            long elapsed = elapsedMs(startNanos);
            log.error("ChromaDB heartbeat failed", e);
            return new CheckResult("ChromaDB", Status.FAILED, elapsed, friendly(e));
        }
    }

    private CheckResult checkOllama() {
        long startNanos = System.nanoTime();
        try {
            Set<String> running = new OllamaClient().listRunningModels();
            long elapsed = elapsedMs(startNanos);

            Set<String> required = new LinkedHashSet<>();
            required.add(OllamaClient.EMBEDDING_MODEL);
            required.add(LLMClient.LLM_MODEL);

            List<String> missing = required.stream().filter(m -> !running.contains(m)).toList();

            if(missing.isEmpty()) {
                log.info("Ollama heartbeat OK (all required models loaded, {} ms)", elapsed);
                return new CheckResult("Ollama", Status.OK, elapsed,
                        "running: " + String.join(", ", required));
            }

            log.warn("Ollama heartbeat DEGRADED: required models not currently loaded: {}", missing);
            return new CheckResult("Ollama", Status.DEGRADED, elapsed,
                    "not currently loaded: " + String.join(", ", missing));
        }
        catch (Exception e) {
            long elapsed = elapsedMs(startNanos);
            log.error("Ollama heartbeat failed", e);
            return new CheckResult("Ollama", Status.FAILED, elapsed, friendly(e));
        }
    }

    private static void renderReport(List<CheckResult> results) {
        TerminalRenderer.println();
        TerminalRenderer.println("  %s", TerminalRenderer.bold("Service heartbeat:"));
        TerminalRenderer.println();
        TerminalRenderer.println("  %s  %s  %s  %s",
                TerminalRenderer.dim(pad("Service", NAME_WIDTH)),
                TerminalRenderer.dim(pad("Status", STATUS_WIDTH)),
                TerminalRenderer.dim(pad("Latency", LATENCY_WIDTH)),
                TerminalRenderer.dim("Details"));
        TerminalRenderer.println("  %s  %s  %s  %s",
                TerminalRenderer.dim("-".repeat(NAME_WIDTH)),
                TerminalRenderer.dim("-".repeat(STATUS_WIDTH)),
                TerminalRenderer.dim("-".repeat(LATENCY_WIDTH)),
                TerminalRenderer.dim("-".repeat(40)));

        for(CheckResult r : results) {
            TerminalRenderer.println("  %s  %s  %s  %s",
                    pad(r.name(), NAME_WIDTH),
                    TerminalRenderer.color(r.status().color, pad(r.status().glyph + " " + r.status().label, STATUS_WIDTH)),
                    TerminalRenderer.dim(pad(r.elapsedMs() + " ms", LATENCY_WIDTH)),
                    r.detail());
        }

        TerminalRenderer.println();
        renderSummary(results);
        TerminalRenderer.println();
    }

    private static void renderSummary(List<CheckResult> results) {
        long healthy  = results.stream().filter(r -> r.status() == Status.OK).count();
        long degraded = results.stream().filter(r -> r.status() == Status.DEGRADED).count();
        long failed   = results.stream().filter(r -> r.status() == Status.FAILED).count();

        Status overall = failed > 0 ? Status.FAILED : (degraded > 0 ? Status.DEGRADED : Status.OK);

        StringBuilder summary = new StringBuilder("  ");
        summary.append(TerminalRenderer.color(overall.color, overall.glyph + " "));
        summary.append(healthy).append("/").append(results.size()).append(" healthy");
        if(degraded > 0) {
            summary.append(", ").append(degraded).append(" degraded");
        }
        if(failed > 0) {
            summary.append(", ").append(failed).append(" failed");
        }
        TerminalRenderer.println("%s", summary.toString());
    }

    private static String pad(String s, int width) {
        if(s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private static String friendly(Throwable t) {
        String msg = t.getMessage();
        return (msg == null || msg.isBlank()) ? t.getClass().getSimpleName() : msg;
    }
}
