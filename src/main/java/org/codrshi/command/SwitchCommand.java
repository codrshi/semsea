package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import org.codrshi.util.WorkspaceDetails;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(
        name = "switch",
        description = "Switch the active workspace.",
        mixinStandardHelpOptions = true
)
public class SwitchCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(SwitchCommand.class);

    @Spec
    CommandSpec commandSpec;

    @Parameters(index = "0", paramLabel = "<workspace>",
            description = "Workspace identifier to switch to.")
    private String target;

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'switch' invoked (target='{}')", target);

        WorkspaceDetails details = DbExecutor.getWorkspaceDetails(target);
        if(details == null) {
            log.warn("Switch target '{}' not found in workspace table", target);
            throw new SemseaException(
                    "Workspace '" + target + "' does not exist.",
                    "Run 'semsea list' to see available workspaces, "
                            + "or 'semsea attach " + target + " --path <dir>' to create it.");
        }

        TerminalRenderer.println();

        if(details.active()) {
            TerminalRenderer.println("  %s workspace %s is already active.",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(target));
            TerminalRenderer.println();

            log.info("'switch' completed: workspace '{}' was already active", target);
        }
        else {
            WorkspaceDetails previous = DbExecutor.getActiveWorkspace();
            String previousId = previous == null ? null : previous.id();

            boolean ok = DbExecutor.setActiveWorkspace(target);
            if(!ok) {
                log.error("setActiveWorkspace returned false for '{}' though row exists", target);
                throw new SemseaException(
                        "Could not activate workspace '" + target + "'.",
                        "Try running 'semsea list' and switching again.");
            }

            TerminalRenderer.println("  %s switched to workspace %s",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(target));
            TerminalRenderer.println("    %s %s",
                    TerminalRenderer.bold("Path:"),
                    TerminalRenderer.dim(details.location()));
            TerminalRenderer.println();

            log.info("'switch' completed: active workspace changed from '{}' to '{}'",
                    previousId, target);
        }

        MetricCollector.print("SWITCH_COMMAND");
    }
}
