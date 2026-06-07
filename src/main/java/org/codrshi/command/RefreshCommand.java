package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.service.IORefreshingService;
import org.codrshi.service.IOService;
import org.codrshi.util.MetadataHolder;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

import java.util.Map;

@Command(
        name = "refresh",
        description = "Re-index changed and removed files in the active workspace.",
        mixinStandardHelpOptions = true
)
public class RefreshCommand implements Runnable {

    private static final Logger log = LogManager.getLogger(RefreshCommand.class);

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
        log.info("'refresh' invoked");

        String workspace = ConfigManager.getConfig().getWorkspace();
        if(workspace == null){
            throw new SemseaException(
                    "No workspace is currently attached.",
                    "Run 'semsea attach <workspace> --path <dir>' first.");
        }

        String path = DbExecutor.getWorkspaceLocation(workspace);
        log.info("Refreshing workspace='{}' at path='{}'", workspace, path);

        TerminalRenderer.println();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Workspace:"),
                TerminalRenderer.cyan(workspace));
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Path:     "),
                TerminalRenderer.dim(path));

        ioRefreshingService.serialize(path);
        DbExecutor.updateLastRefresh(workspace);
        log.info("Updated last_refresh timestamp for workspace '{}'", workspace);

        MetricCollector.print("REFRESH_COMMAND");
        log.info("'refresh' completed for workspace='{}'", workspace);
    }
}
