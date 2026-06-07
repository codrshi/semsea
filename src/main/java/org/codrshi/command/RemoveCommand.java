package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.api.ChromaClient;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

@Command(
        name = "remove",
        description = "Delete a workspace from the index.",
        mixinStandardHelpOptions = true
)
public class RemoveCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(RemoveCommand.class);

    @Spec
    CommandSpec commandSpec;

    @Parameters(index = "0", paramLabel = "<workspace>", description = "Workspace identifier to remove.")
    private String collection;

    private final ChromaClient chromaClient;

    public RemoveCommand() {
        this.chromaClient = new ChromaClient();
    }

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'remove' invoked (workspace='{}')", collection);

        if(!DbExecutor.deleteWorkspaceByID(collection)) {
            throw new SemseaException(
                    "Workspace '" + collection + "' does not exist.",
                    "Run 'semsea' to list available commands.");
        }

        chromaClient.deleteCollection(collection);

        String activeWorkspace = ConfigManager.getConfig().getWorkspace();
        if(activeWorkspace != null && activeWorkspace.equals(collection)) {
            log.info("Cleared active workspace pointer (was '{}')", activeWorkspace);
            ConfigManager.updateWorkspace(null, null);
        }

        TerminalRenderer.println();
        TerminalRenderer.println("  %s workspace %s removed.",
                TerminalRenderer.green("+"),
                TerminalRenderer.bold(collection));
        TerminalRenderer.println();

        MetricCollector.print("REMOVE_COMMAND");
        log.info("'remove' completed for workspace='{}'", collection);
    }
}
