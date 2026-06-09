package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.api.ChromaClient;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "remove",
        description = "Delete one or more workspaces from the index.",
        mixinStandardHelpOptions = true
)
public class RemoveCommand implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(RemoveCommand.class);

    private static final String SEPARATOR = "-".repeat(60);

    @Spec
    CommandSpec commandSpec;

    @Parameters(arity = "1..*", paramLabel = "<workspace>",
            description = "One or more workspace identifiers to remove (space-separated).")
    private List<String> collections;

    private final ChromaClient chromaClient;

    public RemoveCommand() {
        this.chromaClient = new ChromaClient();
    }

    @Override
    public Integer call() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'remove' invoked (workspaces={})", collections);

        TerminalRenderer.println();

        List<String> removed = new ArrayList<>();
        List<String> notFound = new ArrayList<>();

        for(String collection : collections) {
            if(DbExecutor.deleteWorkspaceByID(collection)) {
                chromaClient.deleteCollection(collection);
                removed.add(collection);

                TerminalRenderer.println("  %s workspace %s removed.",
                        TerminalRenderer.green("+"),
                        TerminalRenderer.bold(collection));
                log.info("Removed workspace '{}'", collection);
            }
            else {
                notFound.add(collection);

                TerminalRenderer.println("  %s workspace %s does not exist.",
                        TerminalRenderer.red("x"),
                        TerminalRenderer.bold(collection));
                log.warn("Workspace '{}' not found; skipping", collection);
            }
        }

        if(collections.size() > 1) {
            TerminalRenderer.println("  %s", TerminalRenderer.dim(SEPARATOR));
            TerminalRenderer.println("  %s %d removed, %d not found",
                    TerminalRenderer.green("+"),
                    removed.size(),
                    notFound.size());
        }

        TerminalRenderer.println();

        MetricCollector.print("REMOVE_COMMAND");
        log.info("'remove' completed (removed={}, notFound={})", removed, notFound);

        return notFound.isEmpty() ? 0 : 1;
    }
}
