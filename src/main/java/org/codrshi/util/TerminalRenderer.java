package org.codrshi.util;

import picocli.CommandLine.Model.CommandSpec;

import java.io.PrintWriter;

public class TerminalRenderer {
    private static TerminalRenderer renderer;
    private PrintWriter out;

    private TerminalRenderer(PrintWriter out) {
        this.out = out;
    }

    public static TerminalRenderer init(PrintWriter out) {
        if(renderer == null) {
            renderer = new TerminalRenderer(out);
        }

        return renderer;
    }

    public static TerminalRenderer get() {
        return renderer;
    }

    public void print(String message, Object ... args) {
        out.printf(message, args);
        out.flush();
    }

    public void processing(String fileName) {

        out.printf(
                "\r\033[K⏳ Processing %s...",
                fileName
        );

        out.flush();
    }

    public void completed(String fileName, long millis) {

        out.printf(
                "\r\033[K✅ %s mounted (%.1fs)%n",
                fileName,
                millis / 1000.0
        );

        out.flush();
    }

    public void failed(String fileName, Exception ex) {

        out.printf(
                "\r\033[K❌ Failed %s (%s)%n",
                fileName,
                ex.getMessage()
        );

        out.flush();
    }

    public void finished(String workspace) {

        out.printf(
                "%nMounted '%s' successfully.%n",
                workspace
        );

        out.printf(
                "Currently pointing to '%s'%n",
                workspace
        );

        out.flush();
    }
}
