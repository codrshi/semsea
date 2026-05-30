package org.codrshi.command;

import org.codrshi.service.MountService;
import org.codrshi.util.TerminalRenderer;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Command;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;

// TODO: exit automatically when user changes branch of project (rebouncing)
// TODO: use WatcherService API to continously monitor for system changes.
@Command(name = "attach")
public class AttachCommand implements Runnable {

    @Spec
    CommandSpec commandSpec;

    @Parameters(index = "0")
    private String collection;

    @Option(names = "--clear", defaultValue = "false")
    private boolean clear;

    @Option(names = "--path", defaultValue = "")
    private String path;

    private final MountService mountService;

    private TerminalRenderer terminalRenderer;

    public AttachCommand() {
        mountService = new MountService();
    }

    @Override
    public void run() {

        terminalRenderer = TerminalRenderer.init(commandSpec.commandLine().getOut());

        if(clear) {
            mountService.unmount(collection);
            terminalRenderer.print("Cleared %s from registry.\n", collection);
        }

        terminalRenderer.print("Mounting %s into registry.\n", collection);

        try {
            mountService.mount(collection, path);
        } catch (IOException e) {
            mountService.unmount(collection);
            //throw new RuntimeException(e);
            terminalRenderer.print("Failed to mount %s from registry.\n", collection);
        }

        terminalRenderer.print("Successfully mounted %s. Currently pointing to %s.\n", collection, collection);

    }
}
