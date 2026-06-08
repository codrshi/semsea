package org.codrshi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.command.RootCommand;
import org.codrshi.error.ErrorHandler;
import org.codrshi.repository.DbManager;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class App {

    private static final Logger log = LogManager.getLogger(App.class);

    static void main(String[] args) {
        TerminalRenderer.init(new PrintWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true));

        log.info("semsea starting (args={})", Arrays.toString(args));

        try {
            DbManager.init();
        }
        catch (Throwable t) {
            System.exit(ErrorHandler.report(t, "startup"));
            return;
        }

        ErrorHandler errorHandler = new ErrorHandler();
        int exitCode = new CommandLine(new RootCommand())
                .setExecutionExceptionHandler(errorHandler)
                .setParameterExceptionHandler(errorHandler)
                .execute(args);

        log.info("semsea exiting with code {}", exitCode);
        System.exit(exitCode);
    }
}
