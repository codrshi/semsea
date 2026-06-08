package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;
import org.codrshi.error.SemseaException;
import org.codrshi.metric.MetricCollector;
import org.codrshi.metric.MetricType;
import org.codrshi.metric.Timer;
import org.codrshi.util.ProgressRenderer;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public abstract class IOService {
    private static final Logger log = LogManager.getLogger(IOService.class.getName());

    protected final BatchService batchService;
    protected final DbBatchService dbBatchService;

    public IOService() {
        batchService = new BatchService();
        dbBatchService = new DbBatchService();
    }

    public void serialize(String path) {
        long startNanos = Timer.start();
        Path projectPath = Path.of(path);

        log.info("{} started at '{}'", getProgressTitle(), projectPath);
        ProgressRenderer.get().begin(getProgressTitle());

        try {
            Files.walkFileTree(projectPath, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    Objects.requireNonNull(dir);
                    if(ConfigManager.getConfig().getIgnoredDirectories().contains(dir.getFileName().toString())){
                        log.debug("Ignoring directory {}", dir);
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if(!attrs.isRegularFile() || ConfigManager.getConfig().getIgnoredFiles().contains(file.getFileName().toString())) {
                        log.debug("Ignoring file {}", file);
                        return FileVisitResult.CONTINUE;
                    }

                    String fileType = getFileType(file.getFileName().toString());
                    if(fileType==null) {
                        log.debug("Unsupported file type {}", file);
                        return FileVisitResult.CONTINUE;
                    }

                    long fileStartNanos = Timer.start();
                    processFile(projectPath, file, fileType);
                    MetricCollector.record(MetricType.FILE_PROCESSING, Timer.stop(fileStartNanos));

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, IOException exc) {
                    log.warn("Could not access file '{}'; skipping", file, exc);
                    return FileVisitResult.CONTINUE;
                }
            });

            flush();
            ProgressRenderer.get().end();
            log.info("{} finished at '{}' in {} ms",
                    getProgressTitle(), projectPath, Timer.stop(startNanos) / 1_000_000);
        }
        catch (SemseaException e) {
            ProgressRenderer.get().abort();
            throw e;
        }
        catch (IOException e) {
            ProgressRenderer.get().abort();
            log.error("Failed to walk workspace at '{}'", projectPath, e);
            throw new SemseaException("Failed to read files from '" + projectPath + "'.", e);
        }
        catch (RuntimeException e) {
            ProgressRenderer.get().abort();
            log.error("Indexing failed for workspace at '{}'", projectPath, e);
            throw new SemseaException("Indexing failed.", e);
        }
        finally {
            MetricCollector.record(MetricType.FILE_TRAVERSAL, Timer.stop(startNanos));
        }
    }

    private String getFileType(String fileName) {
        return ConfigManager.getConfig().getSupportedFiles()
                .stream().filter(fileName::endsWith)
                .findFirst().orElse(null);
    }

    protected abstract String getProgressTitle();

    protected abstract void processFile(Path projectPath, Path filePath, String fileType)  throws IOException;

    protected abstract void flush();
}
