package org.codrshi.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.util.SemseaPaths;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashSet;
import java.util.Set;

public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);

    private static final String FILE_NAME = "semsea.json";
    private static final Path FILE_PATH = SemseaPaths.home().resolve(FILE_NAME);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SemseaConfig semseaConfig;

    static {
        if(!Files.exists(FILE_PATH)) {
            bootstrap(FILE_PATH);
        }
        try {
            semseaConfig = objectMapper.readValue(FILE_PATH.toFile(), SemseaConfig.class);
            log.info("Loaded configuration from {}", FILE_PATH);
        }
        catch (Exception e) {
            log.error("Failed to parse {}", FILE_PATH, e);
            throw new SemseaException(
                    "Configuration file '" + FILE_PATH + "' is invalid or corrupted.",
                    e);
        }
    }

    /**
     * Creates the config file at its target home location. If a legacy
     * configuration is found in the current working directory (the
     * pre-move-to-XDG-style behaviour), it is migrated automatically so the
     * existing state is preserved on first run.
     */
    private static void bootstrap(Path target) {
        Path legacy = Paths.get(System.getProperty("user.dir"), FILE_NAME);
        if(Files.exists(legacy)) {
            try {
                Files.copy(legacy, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Migrated legacy configuration from {} to {}", legacy, target);
                return;
            }
            catch (Exception e) {
                log.warn("Failed to migrate legacy configuration from {}", legacy, e);
            }
        }

        log.error("Configuration file not found at {}", target);
        throw new SemseaException(
                "Configuration file '" + FILE_NAME + "' was not found at " + target + ".",
                "Run semsea from the project directory once to migrate it, "
                + "or copy a semsea.json into that location.");
    }

    public static void updateIndexingRules(Set<String> ignoredDirs,    boolean replaceDirs,
                                           Set<String> ignoredFiles,   boolean replaceFiles,
                                           Set<String> supportedFiles, boolean replaceSupported) {

        if(ignoredDirs != null) {
            semseaConfig.setIgnoredDirectories(
                    merge(semseaConfig.getIgnoredDirectories(), ignoredDirs, replaceDirs));
        }
        if(ignoredFiles != null) {
            semseaConfig.setIgnoredFiles(
                    merge(semseaConfig.getIgnoredFiles(), ignoredFiles, replaceFiles));
        }
        if(supportedFiles != null) {
            semseaConfig.setSupportedFiles(
                    merge(semseaConfig.getSupportedFiles(), supportedFiles, replaceSupported));
        }
        save(semseaConfig);
        log.info("Indexing rules updated in {}", FILE_PATH);
    }

    private static Set<String> merge(Set<String> current, Set<String> incoming, boolean replace) {
        Set<String> result = new LinkedHashSet<>();
        if(!replace && current != null) {
            result.addAll(current);
        }
        result.addAll(incoming);
        return result;
    }

    public static void save(SemseaConfig semseaConfig) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH.toFile(), semseaConfig);
            log.debug("Persisted configuration changes to {}", FILE_PATH);
        }
        catch (Exception e) {
            log.error("Failed to write {}", FILE_PATH, e);
            throw new SemseaException("Could not save changes to '" + FILE_PATH + "'.", e);
        }
    }

    public static SemseaConfig getConfig() {
        return semseaConfig;
    }

    public static Path getFilePath() {
        return FILE_PATH;
    }
}
