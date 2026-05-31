package org.codrshi.config;

import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ConfigManager {
    private static final String FILE_NAME = "semsea.json";
    private static final Path FILE_PATH = Paths.get(System.getProperty("user.dir"),FILE_NAME);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final SemseaConfig semseaConfig;

    static {
        try {
            if(!Files.exists(FILE_PATH)){
                throw new RuntimeException(FILE_NAME + " file does not exist.");
            }
            semseaConfig = objectMapper.readValue(FILE_PATH.toFile(), SemseaConfig.class);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to load "+FILE_NAME,e);
        }
    }

    public static void updateWorkspace(String workspace, String collectionId){
        semseaConfig.setWorkspace(workspace);
        semseaConfig.setCollectionId(collectionId);
        save(semseaConfig);
    }

    public static void save(SemseaConfig semseaConfig){
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(FILE_PATH.toFile(), semseaConfig);
        }
        catch (Exception e){
            throw new RuntimeException("Failed to update "+FILE_NAME,e);
        }
    }

    public static SemseaConfig getConfig(){
        return semseaConfig;
    }
}
