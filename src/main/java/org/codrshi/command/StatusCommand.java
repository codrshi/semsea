package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

        WorkspaceDetails active = DbExecutor.getActiveWorkspace();
        if(active == null) {
            renderNotAttached();
            MetricCollector.print("STATUS_COMMAND");
            log.info("'status' completed: no workspace attached");
            return;
        }

        renderStatus(active);

        MetricCollector.print("STATUS_COMMAND");
        log.info("'status' completed for workspace='{}'", active.id());
    }

    private static void renderNotAttached() {
        TerminalRenderer.println();
        TerminalRenderer.println("  %s no workspace is currently attached.",
                TerminalRenderer.dim("-"));
        TerminalRenderer.println("  %s",
                TerminalRenderer.dim("Run 'semsea attach <workspace> --path <dir>' or 'semsea switch <workspace>' to attach one."));
        TerminalRenderer.println();
    }

    private static void renderStatus(WorkspaceDetails details) {
        TerminalRenderer.println();
        printField("Active workspace:", TerminalRenderer.cyan(details.id()));
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
