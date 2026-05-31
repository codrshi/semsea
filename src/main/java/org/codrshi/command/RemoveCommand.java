package org.codrshi.command;

import org.codrshi.api.ChromaClient;
import org.codrshi.config.ConfigManager;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Command;

@Command(name = "remove")
public class RemoveCommand implements Runnable {

    @Spec
    CommandSpec commandSpec;

    @Parameters(index = "0")
    private String collection;

    private final ChromaClient chromaClient;

    public RemoveCommand() {
        this.chromaClient = new ChromaClient();
    }

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());

        if(!DbExecutor.deleteWorkspaceByID(collection)){
            TerminalRenderer.print("Workspace \"%s\" does not exist.",  collection);
            return;
        }

        chromaClient.deleteCollection(collection);
        if(ConfigManager.getConfig().getWorkspace().equals(collection)){
            ConfigManager.updateWorkspace(null, null);
        }

        TerminalRenderer.print("Workspace \"%s\" deleted successfully.",  collection);
    }
}
