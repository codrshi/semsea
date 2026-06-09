package org.codrshi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Resolves the directory that holds semsea's persistent state (config file and
 * SQLite database). The location is stable regardless of the current working
 * directory so 'semsea' can be invoked from anywhere.
 *
 * Precedence:
 *   1. SEMSEA_HOME environment variable, if set and non-blank.
 *   2. OS-appropriate user directory:
 *        Windows : %APPDATA%\semsea     (fallback: ~/.semsea)
 *        macOS   : ~/Library/Application Support/semsea
 *        Linux   : $XDG_CONFIG_HOME/semsea, else ~/.config/semsea
 */
public final class SemseaPaths {

    private static final Logger log = LogManager.getLogger(SemseaPaths.class);

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
            log.debug("Using semsea home from {} -> {}", ENV_HOME, override);
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
            log.info("Created semsea home directory at {}", dir);
        }
        catch (Exception e) {
            log.error("Failed to create semsea home directory at {}", dir, e);
            throw new SemseaException(
                    "Could not create semsea home directory at '" + dir + "'.",
                    "Set the " + ENV_HOME + " environment variable to a writable location.",
                    e);
        }
    }
}
