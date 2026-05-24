package org.codrshi.config;

import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class SemseaConfig{
    private String workspace;
    private Set<String> ignoredDirectories;
    private Set<String> ignoredFiles;
    private Set<String> supportedFiles;
    private int maxChunkSize;
    private int maxChunkLines;
    private int batchSize;
    private int limitMultiplier;
}
