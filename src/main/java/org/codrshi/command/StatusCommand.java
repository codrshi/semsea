package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import org.codrshi.util.TimeFormatter;
import org.codrshi.util.WorkspaceDetails;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

@Command(
        name = "status",
        description = "Show the active workspace and its index status.",
        mixinStandardHelpOptions = true
)
public class StatusCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(StatusCommand.class);

    private static final int LABEL_WIDTH = 17;

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
        printField("Last refreshed:",   TimeFormatter.formatLastRefresh(details.lastRefresh()));
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
}
