package org.codrshi.command;

import org.codrshi.service.MountService;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.IOException;

@Command(name = "attach")
public class AttachCommand implements Runnable {

    @Parameters(index = "0")
    private String collection;

    @Option(names = "--clear", defaultValue = "false")
    private boolean clear;

    @Option(names = "--path", defaultValue = "")
    private String path;

    private final MountService mountService;

    public AttachCommand() {
        mountService = new MountService();
    }

    @Override
    public void run() {

        if(clear) {
            mountService.unmount(collection);
        }

        try {
            mountService.mount(collection, path);
        } catch (IOException e) {
            mountService.unmount(collection);
            throw new RuntimeException(e);
        }

    }
}
