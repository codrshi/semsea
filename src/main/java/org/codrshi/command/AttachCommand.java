package org.codrshi.command;

import org.codrshi.metric.MetricCollector;
import org.codrshi.service.MountService;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;
import java.nio.file.Path;

// TODO: exit automatically when user changes branch of project (rebouncing)
// TODO: use WatcherService API to continously monitor for system changes.
@Command(
        name = "attach",
        description = "Index a workspace into the vector store.",
        mixinStandardHelpOptions = true
)
public class AttachCommand implements Runnable {

    @Spec
    CommandSpec commandSpec;

    @Parameters(index = "0", paramLabel = "<workspace>", description = "Workspace identifier to attach.")
    private String collection;

    @Option(names = "--clear", defaultValue = "false",
            description = "Remove any existing index at --path before attaching.")
    private boolean clear;

    @Option(names = "--path", defaultValue = "",
            description = "Workspace directory (default: current directory).")
    private String path;

    private final MountService mountService;

    public AttachCommand() {
        mountService = new MountService();
    }

    @Override
    public void run() {

        TerminalRenderer.init(commandSpec.commandLine().getOut());

        String absolutePath;
        try {
            absolutePath = Path.of(path).toRealPath().toString();
        }
        catch (IOException e) {
            TerminalRenderer.println();
            TerminalRenderer.println("  %s could not resolve path %s",
                    TerminalRenderer.red("✗"),
                    TerminalRenderer.dim(path.isEmpty() ? "<current directory>" : path));
            TerminalRenderer.println();
            return;
        }

        TerminalRenderer.println();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Workspace:"),
                TerminalRenderer.cyan(collection));
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Path:     "),
                TerminalRenderer.dim(absolutePath));

        if(clear) {
            mountService.unmount(path);
            TerminalRenderer.println("  %s cleared previous index at this path",
                    TerminalRenderer.gray("⊖"));
        }

        try {
            mountService.mount(collection, path);
        } catch (IOException e) {
            TerminalRenderer.println();
            TerminalRenderer.println("  %s failed to attach workspace %s",
                    TerminalRenderer.red("✗"),
                    TerminalRenderer.bold(collection));
            TerminalRenderer.println();
        }

        MetricCollector.print("ATTACH_COMMAND");
    }
}
