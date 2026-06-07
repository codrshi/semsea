package org.codrshi.command;

import org.codrshi.config.ConfigManager;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.service.IORefreshingService;
import org.codrshi.service.IOService;
import org.codrshi.util.MetadataHolder;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.io.IOException;
import java.util.Map;

@Command(
        name = "refresh",
        description = "Re-index changed and removed files in the active workspace.",
        mixinStandardHelpOptions = true
)
public class RefreshCommand implements Runnable {

    @Spec
    CommandSpec commandSpec;

    private final IOService ioRefreshingService;

    public RefreshCommand() {
        Map<String, MetadataHolder> metadataHolderMap = DbExecutor.loadMetadata(ConfigManager.getConfig().getWorkspace());
        ioRefreshingService = new IORefreshingService(metadataHolderMap);
    }

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());

        String workspace = ConfigManager.getConfig().getWorkspace();
        if(workspace == null){
            TerminalRenderer.println();
            TerminalRenderer.println("  %s no workspace is currently attached.",
                    TerminalRenderer.red("✗"));
            TerminalRenderer.println("  %s",
                    TerminalRenderer.dim("Run 'semsea attach <workspace> --path <dir>' first."));
            TerminalRenderer.println();
            return;
        }

        String path = DbExecutor.getWorkspaceLocation(workspace);

        TerminalRenderer.println();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Workspace:"),
                TerminalRenderer.cyan(workspace));
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Path:     "),
                TerminalRenderer.dim(path));

        try {
            ioRefreshingService.serialize(path);
        } catch (IOException e) {
            TerminalRenderer.println();
            TerminalRenderer.println("  %s failed to refresh workspace %s",
                    TerminalRenderer.red("✗"),
                    TerminalRenderer.bold(workspace));
            TerminalRenderer.println();
            throw new RuntimeException(e);
        }

        MetricCollector.print("REFRESH_COMMAND");
    }
}
