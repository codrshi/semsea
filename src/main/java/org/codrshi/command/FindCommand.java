package org.codrshi.command;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.repository.DbExecutor;
import org.codrshi.service.QueryService;
import org.codrshi.util.TerminalRenderer;
import org.codrshi.util.WorkspaceDetails;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

import java.util.List;
import java.util.concurrent.Callable;

@Command(
        name = "find",
        description = "Semantically search files in the active workspace.",
        mixinStandardHelpOptions = true
)
public class FindCommand implements Callable<Integer> {

    private static final Logger log = LogManager.getLogger(FindCommand.class);

    private static final int FILE_COLUMN_WIDTH = 60;

    @Spec
    CommandSpec commandSpec;

    // TODO: set limit to query length
    @Parameters(index = "0", paramLabel = "<query>", description = "Natural language description of the file you want to find.")
    private String query;

    @Option(names = "--limit", defaultValue = "5", description = "Max number of results to show (default: 5).")
    private int limit;

    private final QueryService queryService;

    public FindCommand(){
        queryService = new QueryService();
    }

    @Override
    public Integer call() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());
        log.info("'find' invoked (query=\"{}\", limit={})", query, limit);

        WorkspaceDetails active = DbExecutor.getActiveWorkspace();
        if(active == null) {
            throw new SemseaException(
                    "No workspace is currently attached.",
                    "Run 'semsea attach <workspace> --path <dir>' first.");
        }

        List<List<String>> result = queryService.search(active.collectionId(), query, limit);

        TerminalRenderer.println();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Query:"),
                "\"" + query + "\"");
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.bold("Scope:"),
                TerminalRenderer.cyan(active.id()));
        TerminalRenderer.println();

        if(result.isEmpty()) {
            TerminalRenderer.println("  %s no matching files found.",
                    TerminalRenderer.dim("-"));
            TerminalRenderer.println();
            log.info("'find' completed: 0 results for query=\"{}\"", query);
            MetricCollector.print("FIND_COMMAND");
            return 0;
        }

        printHeader();
        for(int i = 0; i < result.size(); i++) {
            List<String> row = result.get(i);
            String file = row.getFirst();
            String lastModified = row.size() > 1 ? row.get(1) : "";
            TerminalRenderer.println("  %s  %-" + FILE_COLUMN_WIDTH + "s  %s",
                    TerminalRenderer.dim(String.format("%2d", i + 1)),
                    truncate(file, FILE_COLUMN_WIDTH),
                    TerminalRenderer.dim(lastModified));
        }
        TerminalRenderer.println();

        log.info("'find' completed: {} results for query=\"{}\"", result.size(), query);
        MetricCollector.print("FIND_COMMAND");
        return 0;
    }

    private static void printHeader() {
        TerminalRenderer.println("  %s  %s  %s",
                TerminalRenderer.dim(" #"),
                TerminalRenderer.bold(pad("File", FILE_COLUMN_WIDTH)),
                TerminalRenderer.bold("Last Modified"));
        TerminalRenderer.println("  %s  %s  %s",
                TerminalRenderer.dim("--"),
                TerminalRenderer.dim("-".repeat(FILE_COLUMN_WIDTH)),
                TerminalRenderer.dim("-".repeat(24)));
    }

    private static String pad(String s, int width) {
        if(s.length() >= width) return s;
        return s + " ".repeat(width - s.length());
    }

    private static String truncate(String s, int max) {
        if(s.length() <= max) return pad(s, max);
        return "..." + s.substring(s.length() - max + 3);
    }
}
