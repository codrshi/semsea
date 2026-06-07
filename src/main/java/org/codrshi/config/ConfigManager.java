package org.codrshi.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.error.SemseaException;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {

    private static final Logger log = LogManager.getLogger(ConfigManager.class);

    private static final String FILE_NAME = "semsea.json";
    private static final Path FILE_PATH = Paths.get(System.getProperty("user.dir"), FILE_NAME);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SemseaConfig semseaConfig;

    static {
        if(!Files.exists(FILE_PATH)) {
            log.error("Configuration file not found at {}", FILE_PATH);
            throw new SemseaException(
                    "Configuration file '" + FILE_NAME + "' was not found in the current directory.",
                    "Create " + FILE_NAME + " in the directory you are running semsea from.");
        }
        try {
            semseaConfig = objectMapper.readValue(FILE_PATH.toFile(), SemseaConfig.class);
        }
        catch (Exception e) {
            log.error("Failed to parse {}", FILE_PATH, e);
            throw new SemseaException(
                    "Configuration file '" + FILE_NAME + "' is invalid or corrupted.",
                    e);
        }
    }

    public static void updateWorkspace(String workspace, String collectionId) {
        semseaConfig.setWorkspace(workspace);
        semseaConfig.setCollectionId(collectionId);
        save(semseaConfig);
    }

    public static void save(SemseaConfig semseaConfig) {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH.toFile(), semseaConfig);
        }
        catch (Exception e) {
            log.error("Failed to write {}", FILE_PATH, e);
            throw new SemseaException("Could not save changes to '" + FILE_NAME + "'.", e);
        }
    }

    public static SemseaConfig getConfig() {
        return semseaConfig;
    }
}
