package org.codrshi.command;

import org.codrshi.api.ChromaClient;
import org.codrshi.config.ConfigManager;
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

        TerminalRenderer.println();

        if(!DbExecutor.deleteWorkspaceByID(collection)){
            TerminalRenderer.println("  %s workspace %s does not exist.",
                    TerminalRenderer.red("x"),
                    TerminalRenderer.bold(collection));
            TerminalRenderer.println();
            return;
        }

        chromaClient.deleteCollection(collection);
        String activeWorkspace = ConfigManager.getConfig().getWorkspace();
        if(activeWorkspace != null && activeWorkspace.equals(collection)){
            ConfigManager.updateWorkspace(null, null);
        }

        TerminalRenderer.println("  %s workspace %s removed.",
                TerminalRenderer.green("+"),
                TerminalRenderer.bold(collection));
        TerminalRenderer.println();

        MetricCollector.print("REMOVE_COMMAND");
    }
}
