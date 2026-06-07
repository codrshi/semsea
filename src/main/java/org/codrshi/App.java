package org.codrshi;

import org.codrshi.command.RootCommand;
import org.codrshi.error.ErrorHandler;
import org.codrshi.repository.DbManager;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine;

import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public class App {

    static void main(String[] args) {
        TerminalRenderer.init(new PrintWriter(
                new OutputStreamWriter(System.out, StandardCharsets.UTF_8), true));

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

        System.exit(exitCode);
    }
}
