package org.codrshi.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import org.codrshi.util.SemseaPaths;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);

    private static final String FILE_NAME = "semsea.json";
    private static final String BUNDLED_DEFAULT = "/semsea.default.json";
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
     * First-run bootstrap: materialises {@code semsea.json} at its home location by
     * copying the default file shipped inside the JAR.
     */
    private static void bootstrap(Path target) {
        try (InputStream in = ConfigManager.class.getResourceAsStream(BUNDLED_DEFAULT)) {
            if(in == null) {
                throw new SemseaException(
                        "Bundled default configuration was not found inside the semsea jar.",
                        "Reinstall semsea from a fresh distribution archive.");
            }
            Files.copy(in, target);
            log.info("Initialised semsea configuration at {}", target);
        }
        catch (SemseaException e) {
            throw e;
        }
        catch (Exception e) {
            log.error("Failed to write initial configuration to {}", target, e);
            throw new SemseaException(
                    "Could not create configuration at '" + target + "'.",
                    "Check that the parent directory is writable.",
                    e);
        }
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
