package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import org.codrshi.util.TimeFormatter;
import org.codrshi.util.WorkspaceDetails;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.List;

@Command(
        name = "list",
        description = "List all workspaces with their path and last refreshed time.",
        mixinStandardHelpOptions = true
)
public class ListCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(ListCommand.class);

    private static final int LABEL_WIDTH = 17;

    @Spec
    CommandSpec commandSpec;

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'list' invoked");

        List<WorkspaceDetails> workspaces = DbExecutor.listAllWorkspaces();
        String active = ConfigManager.getConfig().getWorkspace();

        TerminalRenderer.println();

        if(workspaces.isEmpty()) {
            TerminalRenderer.println("  %s no workspaces registered.",
                    TerminalRenderer.dim("-"));
            TerminalRenderer.println("  %s",
                    TerminalRenderer.dim("Run 'semsea attach <workspace> --path <dir>' to create one."));
            TerminalRenderer.println();
            log.info("'list' completed: no workspaces found");
            MetricCollector.print("LIST_COMMAND");
            return;
        }

        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Workspaces:"),
                TerminalRenderer.dim("(" + workspaces.size() + ")"));
        TerminalRenderer.println();

        for(int i = 0; i < workspaces.size(); i++) {
            renderEntry(i + 1, workspaces.get(i), active);
            if(i < workspaces.size() - 1) {
                TerminalRenderer.println();
            }
        }

        TerminalRenderer.println();

        MetricCollector.print("LIST_COMMAND");
        log.info("'list' completed: {} workspace(s)", workspaces.size());
    }

    private static void renderEntry(int index, WorkspaceDetails w, String activeId) {
        boolean isActive = w.id().equals(activeId);

        String activeMarker = isActive
                ? "  " + TerminalRenderer.green("(active)")
                : "";

        TerminalRenderer.println("  %s%s %s%s",
                TerminalRenderer.dim(String.format("%2d.", index)),
                " ",
                TerminalRenderer.cyan(w.id()),
                activeMarker);

        printField("Path:",           TerminalRenderer.dim(w.location()));
        printField("Last refreshed:", TimeFormatter.formatLastRefresh(w.lastRefresh()));
    }

    private static void printField(String label, String value) {
        TerminalRenderer.println("      %s %s",
                TerminalRenderer.bold(padLabel(label)),
                value);
    }

    private static String padLabel(String label) {
        if(label.length() >= LABEL_WIDTH) return label;
        return label + " ".repeat(LABEL_WIDTH - label.length());
    }
}
