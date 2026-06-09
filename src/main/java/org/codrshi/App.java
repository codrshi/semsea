package org.codrshi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.command.RootCommand;
import org.codrshi.error.ErrorHandler;
import org.codrshi.repository.DbManager;
import org.codrshi.util.SemseaPaths;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class App {

    /*
     * IMPORTANT: this block must execute BEFORE the {@code log} field below is
     * initialised. log4j2.xml references {@code ${sys:semsea.home}} to decide
     * where {@code app-output.log} goes, and that property must be set before
     * log4j's first configuration read (triggered by {@link LogManager#getLogger}).
     */
    static {
        System.setProperty("semsea.home", SemseaPaths.home().toString());
    }

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
