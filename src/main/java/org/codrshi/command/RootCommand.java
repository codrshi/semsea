package org.codrshi.command;

import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

// TODO: command to check heartbeat of OllamaClient & ChromaClient
@Command(
        name = "semsea",
        version = "semsea 1.0",
        mixinStandardHelpOptions = true,
        description = "Semantic codebase search powered by a local LLM and vector store.",
        subcommands = {
                AttachCommand.class,
                StatusCommand.class,
                ListCommand.class,
                SwitchCommand.class,
                FindCommand.class,
                RefreshCommand.class,
                RemoveCommand.class
        }
)
public class RootCommand implements Runnable {

    @Spec
    CommandSpec commandSpec;

    @Override
    public void run() {
        TerminalRenderer.init(commandSpec.commandLine().getOut());

        TerminalRenderer.println();
        TerminalRenderer.println("  %s  %s",
                TerminalRenderer.bold("semsea"),
                TerminalRenderer.dim("v1.0"));
        TerminalRenderer.println("  %s", "Semantic codebase search powered by a local LLM and vector store.");
        TerminalRenderer.println();

        TerminalRenderer.println("  %s", TerminalRenderer.bold("Usage"));
        TerminalRenderer.println("    semsea <command> [arguments] [options]");
        TerminalRenderer.println();

        TerminalRenderer.println("  %s", TerminalRenderer.bold("Commands"));
        printCommand("attach <workspace>",     "Index a workspace into the vector store");
        printOption ("--path <dir>",           "Workspace directory (default: current directory)");
        printOption ("--clear",                "Remove any existing index at --path before attaching");
        printCommand("status",                 "Show the active workspace and its index status");
        printCommand("list",                   "List all workspaces with their path and last refreshed time");
        printCommand("switch <workspace>",     "Switch the active workspace");
        printCommand("find <query>",           "Semantically search files in the active workspace");
        printOption ("--limit <n>",            "Max number of results to show (default: 5)");
        printCommand("refresh",                "Re-index changed and removed files in the active workspace");
        printCommand("remove <workspace>...",  "Delete one or more workspaces from the index");
        TerminalRenderer.println();

        TerminalRenderer.println("  %s", TerminalRenderer.bold("Examples"));
        TerminalRenderer.println("    %s",
                TerminalRenderer.dim("$ ") + "semsea attach myproject --path ./src");
        TerminalRenderer.println("    %s",
                TerminalRenderer.dim("$ ") + "semsea status");
        TerminalRenderer.println("    %s",
                TerminalRenderer.dim("$ ") + "semsea find \"where is the database config initialized\"");
        TerminalRenderer.println("    %s",
                TerminalRenderer.dim("$ ") + "semsea refresh");
        TerminalRenderer.println();

        TerminalRenderer.println("  %s",
                TerminalRenderer.dim("Run 'semsea <command> --help' for command-specific help."));
        TerminalRenderer.println();
    }

    private static void printCommand(String name, String description) {
        TerminalRenderer.println("    %-26s  %s", TerminalRenderer.cyan(name), description);
    }

    private static void printOption(String name, String description) {
        TerminalRenderer.println("      %-24s  %s", TerminalRenderer.dim(name), TerminalRenderer.dim(description));
    }
}
