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

        String current = ConfigManager.getConfig().getWorkspace();

        TerminalRenderer.println();

        if(target.equals(current)) {
            TerminalRenderer.println("  %s workspace %s is already active.",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(target));
            TerminalRenderer.println();

            log.info("'switch' completed: workspace '{}' was already active", target);
        }
        else {
            ConfigManager.updateWorkspace(target, details.collectionId());

            TerminalRenderer.println("  %s switched to workspace %s",
                    TerminalRenderer.green("+"),
                    TerminalRenderer.bold(target));
            TerminalRenderer.println("    %s %s",
                    TerminalRenderer.bold("Path:"),
                    TerminalRenderer.dim(details.location()));
            TerminalRenderer.println();

            log.info("'switch' completed: active workspace changed from '{}' to '{}'",
                    current, target);
        }

        MetricCollector.print("SWITCH_COMMAND");
    }
}
