package org.codrshi.command;

import org.codrshi.config.ConfigManager;
import org.codrshi.metric.MetricCollector;
import org.codrshi.metric.MetricType;
import org.codrshi.metric.Timer;
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
        name = "refresh"
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
            TerminalRenderer.print("Not attached to any workspace.");
            return;
        }

        String path = DbExecutor.getWorkspaceLocation(workspace);

        try {
            ioRefreshingService.serialize(path);
        } catch (IOException e) {
            TerminalRenderer.print("Failed to refresh workspace.");
            throw new RuntimeException(e);
        }

        MetricCollector.print("REFRESH_COMMAND");
    }
}
