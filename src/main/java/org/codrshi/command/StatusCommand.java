package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import org.codrshi.util.WorkspaceDetails;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Command(
        name = "status",
        description = "Show the active workspace and its index status.",
        mixinStandardHelpOptions = true
)
public class StatusCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(StatusCommand.class);

    private static final int LABEL_WIDTH = 17;
    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Spec
    CommandSpec commandSpec;

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'status' invoked");

        String workspace = ConfigManager.getConfig().getWorkspace();
        if(workspace == null) {
            renderNotAttached();
            MetricCollector.print("STATUS_COMMAND");
            log.info("'status' completed: no workspace attached");
            return;
        }

        WorkspaceDetails details = DbExecutor.getWorkspaceDetails(workspace);
        if(details == null) {
            log.warn("Active workspace pointer '{}' has no row in workspace table", workspace);
            throw new SemseaException(
                    "Active workspace '" + workspace + "' is missing from the local database.",
                    "Run 'semsea attach " + workspace + " --path <dir>' to re-attach it.");
        }

        renderStatus(workspace, details);

        MetricCollector.print("STATUS_COMMAND");
        log.info("'status' completed for workspace='{}'", workspace);
    }

    private static void renderNotAttached() {
        TerminalRenderer.println();
        TerminalRenderer.println("  %s no workspace is currently attached.",
                TerminalRenderer.dim("-"));
        TerminalRenderer.println("  %s",
                TerminalRenderer.dim("Run 'semsea attach <workspace> --path <dir>' to attach one."));
        TerminalRenderer.println();
    }

    private static void renderStatus(String workspace, WorkspaceDetails details) {
        TerminalRenderer.println();
        printField("Active workspace:", TerminalRenderer.cyan(workspace));
        printField("Path:",             TerminalRenderer.dim(details.location()));
        printField("Last refreshed:",   formatLastRefresh(details.lastRefresh()));
        TerminalRenderer.println();
    }

    private static void printField(String label, String value) {
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold(padLabel(label)),
                value);
    }

    private static String padLabel(String label) {
        if(label.length() >= LABEL_WIDTH) return label;
        return label + " ".repeat(LABEL_WIDTH - label.length());
    }

    private static String formatLastRefresh(Timestamp ts) {
        if(ts == null) {
            return TerminalRenderer.dim("never");
        }
        Instant when = ts.toInstant();
        String formatted = TIMESTAMP_FORMATTER.format(when.atZone(ZoneId.systemDefault()));
        String relative = formatRelative(Duration.between(when, Instant.now()));
        return formatted + "  " + TerminalRenderer.dim("(" + relative + ")");
    }

    private static String formatRelative(Duration d) {
        long seconds = d.getSeconds();
        if(seconds < 0)  return "in the future";
        if(seconds < 5)  return "just now";
        if(seconds < 60) return seconds + " seconds ago";

        long minutes = seconds / 60;
        if(minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");

        long hours = minutes / 60;
        if(hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");

        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
