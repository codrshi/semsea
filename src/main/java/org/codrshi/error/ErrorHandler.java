package org.codrshi.error;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.util.ProgressRenderer;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;

/**
 * Central exit point for every error path. Responsibilities:
 *   1. Log the full exception (message + stack trace + cause chain) via log4j.
 *   2. Print a single concise, client-friendly line to the terminal.
 *   3. Restore any progress-renderer cursor/state.
 *   4. Return a non-zero exit code.
 *
 * Wired into Picocli as both IExecutionExceptionHandler (run/call throws)
 * and IParameterExceptionHandler (invalid CLI input). For bootstrap errors
 * that happen before Picocli executes, use the static {@link #report(Throwable, String)}.
 */
public class ErrorHandler implements IExecutionExceptionHandler, IParameterExceptionHandler {

    private static final Logger log = LogManager.getLogger(ErrorHandler.class);
    private static final String DEFAULT_HINT = "See logs/app-output.log for details.";
    private static final int EXIT_CODE_ERROR = 1;

    @Override
    public int handleExecutionException(Exception ex, CommandLine cmd, ParseResult parseResult) {
        TerminalRenderer.init(cmd.getOut());
        return report(ex, cmd.getCommandName());
    }

    @Override
    public int handleParseException(ParameterException ex, String[] args) {
        CommandLine cmd = ex.getCommandLine();
        log.debug("Invalid CLI input for '{}': {}", cmd.getCommandName(), ex.getMessage());

        TerminalRenderer.init(cmd.getOut());
        TerminalRenderer.println();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.red("x"),
                ex.getMessage());
        TerminalRenderer.println("  %s",
                TerminalRenderer.dim("Run '" + cmd.getCommandSpec().qualifiedName() + " --help' for usage."));
        TerminalRenderer.println();

        return cmd.getCommandSpec().exitCodeOnInvalidInput();
    }

    /**
     * Reports a failure to the terminal, logs it via log4j, and returns the exit code.
     * Safe to call before Picocli is initialized.
     */
    public static int report(Throwable ex, String context) {
        Throwable rootProblem = unwrap(ex);
        String userMessage;
        String hint;

        if(rootProblem instanceof SemseaException semsea) {
            userMessage = semsea.getMessage();
            hint = semsea.getHint();
            if(hint == null && semsea.getCause() != null) {
                hint = DEFAULT_HINT;
            }
        }
        else {
            userMessage = "An unexpected error occurred.";
            hint = DEFAULT_HINT;
        }

        log.error("'{}' failed", context, ex);

        ProgressRenderer.get().abort();

        TerminalRenderer.println();
        TerminalRenderer.println("  %s %s",
                TerminalRenderer.red("x"),
                userMessage);
        if(hint != null) {
            TerminalRenderer.println("  %s", TerminalRenderer.dim(hint));
        }
        TerminalRenderer.println();

        return EXIT_CODE_ERROR;
    }

    /**
     * Unwrap JVM wrappers (e.g. ExceptionInInitializerError) so that a
     * SemseaException thrown from a static initializer is still treated as one.
     */
    private static Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while(current != null
                && !(current instanceof SemseaException)
                && current.getCause() != null
                && (current instanceof ExceptionInInitializerError
                        || current.getClass() == RuntimeException.class)) {
            current = current.getCause();
        }
        return current == null ? ex : current;
    }
}
