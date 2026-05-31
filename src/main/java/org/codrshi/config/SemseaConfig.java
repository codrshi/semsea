package org.codrshi.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class SemseaConfig{
    private String dbName;
    private String dbDirectory;
    private String dbUrl;
    private int sqliteBatchSize;
    private String workspace;
    private String collectionId;
    private Set<String> ignoredDirectories;
    private Set<String> ignoredFiles;
    private Set<String> supportedFiles;
    private int maxChunkSize;
    private int batchSize;
    private int limitMultiplier;
}
