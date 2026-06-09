package org.codrshi.util;

import org.codrshi.error.SemseaException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the directory that holds semsea's persistent state (config file, SQLite
 * database, logs). The location is stable regardless of the current working
 * directory so 'semsea' can be invoked from anywhere.
 * <p>
 * Precedence:
 * <ol>
 *   <li>{@code SEMSEA_HOME} environment variable, if set and non-blank.</li>
 *   <li>OS-appropriate user data directory:
 *     <ul>
 *       <li>Windows : {@code %APPDATA%\semsea}    (fallback: {@code ~/.semsea})</li>
 *       <li>macOS   : {@code ~/Library/Application Support/semsea}</li>
 *       <li>Linux   : {@code $XDG_CONFIG_HOME/semsea}, else {@code ~/.config/semsea}</li>
 *     </ul>
 *   </li>
 * </ol>
 * <p>
 * Intentionally does <strong>not</strong> hold a static log4j logger so that
 * {@link App}'s startup sequence can call {@link #home()} <em>before</em> log4j
 * is configured (the log file path itself depends on this value).
 */
public final class SemseaPaths {

    private static final String DIR_NAME = "semsea";
    private static final String ENV_HOME = "SEMSEA_HOME";

    private SemseaPaths() { }

    public static Path home() {
        Path home = resolveHome();
        ensureDirectoryExists(home);
        return home;
    }

    private static Path resolveHome() {
        String override = System.getenv(ENV_HOME);
        if(override != null && !override.isBlank()) {
            return Paths.get(override);
        }

        String os = System.getProperty("os.name", "").toLowerCase();
        String userHome = System.getProperty("user.home");

        if(os.contains("win")) {
            String appData = System.getenv("APPDATA");
            if(appData != null && !appData.isBlank()) {
                return Paths.get(appData, DIR_NAME);
            }
            return Paths.get(userHome, "." + DIR_NAME);
        }

        if(os.contains("mac") || os.contains("darwin")) {
            return Paths.get(userHome, "Library", "Application Support", DIR_NAME);
        }

        String xdg = System.getenv("XDG_CONFIG_HOME");
        if(xdg != null && !xdg.isBlank()) {
            return Paths.get(xdg, DIR_NAME);
        }
        return Paths.get(userHome, ".config", DIR_NAME);
    }

    private static void ensureDirectoryExists(Path dir) {
        if(Files.exists(dir)) return;
        try {
            Files.createDirectories(dir);
        }
        catch (Exception e) {
            throw new SemseaException(
                    "Could not create semsea home directory at '" + dir + "'.",
                    "Set the " + ENV_HOME + " environment variable to a writable location.",
                    e);
        }
    }
}
