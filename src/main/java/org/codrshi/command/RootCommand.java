package org.codrshi.command;

import picocli.CommandLine.Command;

// TODO: command to check heartbeat of OllamaClient & ChromaClient
@Command(
        name = "semsea",
        version = "1.0",
        subcommands = {
                AttachCommand.class,
                FindCommand.class,
                RemoveCommand.class,
                RefreshCommand.class
        }
)
public class RootCommand implements Runnable {

    @Override
    public void run() {
        System.out.println("use sub-commands: search, index");
    }
}
