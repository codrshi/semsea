package org.codrshi.util;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * Shared timestamp formatting helpers for terminal output. Renders the
 * absolute timestamp in the JVM default timezone alongside a dim-colored
 * "(N units ago)" relative qualifier.
 */
public final class TimeFormatter {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private TimeFormatter() {}

    /**
     * Returns a string like {@code "2026-06-07 18:32:15  (5 minutes ago)"}
     * where the relative qualifier is rendered with the terminal's dim style.
     * Returns a dim {@code "never"} when {@code ts} is null.
     */
    public static String formatLastRefresh(Timestamp ts) {
        if(ts == null) {
            return TerminalRenderer.dim("never");
        }
        Instant when = ts.toInstant();
        String formatted = TIMESTAMP_FORMATTER.format(when.atZone(ZoneId.systemDefault()));
        String relative = formatRelative(Duration.between(when, Instant.now()));
        return formatted + "  " + TerminalRenderer.dim("(" + relative + ")");
    }

    public static String formatRelative(Duration d) {
        long seconds = d.getSeconds();
        if(seconds < 0)  return "in the future";
        if(seconds < 5)  return "just now";
        if(seconds < 60) return seconds + " seconds ago";

        long minutes = seconds / 60;
        if(minutes < 60) return minutes + (minutes == 1 ? " minute ago" : " minutes ago");

        long hours = minutes / 60;
        if(hours < 24) return hours + (hours == 1 ? " hour ago" : " hours ago");

        long days = hours / 24;
        return days + (days == 1 ? " day ago" : " days ago");
    }
}
