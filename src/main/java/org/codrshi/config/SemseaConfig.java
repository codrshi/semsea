package org.codrshi.config;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SemseaConfig{
    private String workspace;
    private List<String> ignoredDirectories;
    private List<String> supportedFiles;
    private int maxChunkSize;
    private int maxChunkLines;
    private int batchSize;
    private int limitMultiplier;
}
