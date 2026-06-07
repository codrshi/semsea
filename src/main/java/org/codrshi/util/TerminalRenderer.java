package org.codrshi.util;

import java.io.PrintWriter;

public class TerminalRenderer {

    public static final String RESET   = "\033[0m";
    public static final String BOLD    = "\033[1m";
    public static final String DIM     = "\033[2m";
    public static final String RED     = "\033[31m";
    public static final String GREEN   = "\033[32m";
    public static final String YELLOW  = "\033[33m";
    public static final String BLUE    = "\033[34m";
    public static final String MAGENTA = "\033[35m";
    public static final String CYAN    = "\033[36m";
    public static final String GRAY    = "\033[90m";

    private static final String CLEAR_TO_END_OF_SCREEN = "\033[J";
    private static final String CURSOR_HIDE            = "\033[?25l";
    private static final String CURSOR_SHOW            = "\033[?25h";

    private static TerminalRenderer renderer;
    private final PrintWriter out;

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

    public static void println(String message, Object ... args) {
        renderer.out.printf(message, args);
        renderer.out.println();
        renderer.out.flush();
    }

    public static void println() {
        renderer.out.println();
        renderer.out.flush();
    }

    public static void printRaw(String text) {
        renderer.out.print(text);
        renderer.out.flush();
    }

    public static void moveCursorUp(int lines) {
        if(lines <= 0) return;
        renderer.out.printf("\033[%dA", lines);
    }

    public static void clearBelow() {
        renderer.out.print(CLEAR_TO_END_OF_SCREEN);
    }

    public static void hideCursor() {
        renderer.out.print(CURSOR_HIDE);
        renderer.out.flush();
    }

    public static void showCursor() {
        renderer.out.print(CURSOR_SHOW);
        renderer.out.flush();
    }

    public static String color(String color, String text) {
        return color + text + RESET;
    }

    public static String red(String text)    { return color(RED, text); }
    public static String green(String text)  { return color(GREEN, text); }
    public static String yellow(String text) { return color(YELLOW, text); }
    public static String cyan(String text)   { return color(CYAN, text); }
    public static String bold(String text)   { return color(BOLD, text); }
    public static String dim(String text)    { return color(DIM, text); }
    public static String gray(String text)   { return color(GRAY, text); }
}
