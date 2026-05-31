package org.codrshi.util;

import java.io.PrintWriter;

public class TerminalRenderer {
    private static TerminalRenderer renderer;
    private PrintWriter out;

    private TerminalRenderer(PrintWriter out) {
        this.out = out;
    }

    public static void init(PrintWriter out) {
        if(renderer == null) {
            renderer = new TerminalRenderer(out);
        }
    }

    public static TerminalRenderer get() {
        return renderer;
    }

    public static void print(String message, Object ... args) {
        renderer.out.printf(message, args);
        renderer.out.flush();
    }
}
