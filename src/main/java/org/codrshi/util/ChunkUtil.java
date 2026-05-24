package org.codrshi.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.config.ConfigManager;

import java.util.ArrayList;
import java.util.List;

public class ChunkUtil {
    private static final Logger log = LogManager.getLogger(ChunkUtil.class.getName());

    private static final int MAX_LINES = ConfigManager.getConfig().getMaxChunkLines();
    private static final int MAX_CHARS = ConfigManager.getConfig().getMaxChunkSize();

    public static List<String> getChunks(List<String> lines) {
        StringBuilder buffer = new StringBuilder();
        List<String> results = new ArrayList<>();
        int startLine = 0;

        log.debug("Creating chunks of {} lines...", lines.size());

        for(int currentLine = 0; currentLine < lines.size(); currentLine++) {

            if(lines.get(currentLine).length() > MAX_CHARS) {
                addLargeLine(lines.get(currentLine), currentLine, results);
            } else {
                buffer.append(lines.get(currentLine)).append('\n');
            }

            if(shouldSplit(buffer, startLine, currentLine)) {
                results.add(buffer.toString().trim());

                log.debug("Chunk created of {} characters from line {} to {}.", buffer.length(), startLine+1, currentLine+1);

                buffer.setLength(0);
                startLine = currentLine+1;
            }
        }

        if(!buffer.isEmpty()){
            results.add(buffer.toString().trim());
            log.debug("Chunk created of {} characters from line {} to {}.", buffer.length(), startLine+1, lines.size());
        }

        return results;
    }

    private static void addLargeLine(String line, int lineNumber, List<String> results) {
        for(int start = 0; start < line.length(); start+=MAX_CHARS) {
            int end = Math.min(line.length(), start + MAX_CHARS);
            results.add(line.substring(start, end));

            log.debug("Chunk created of {} characters in line {}.", end-start, lineNumber);
        }
    }

    //TODO: language-aware chunking with heuristic boundaries
    private static boolean shouldSplit(StringBuilder buffer, int startLine, int currentLine) {
        return buffer.length() >= MAX_CHARS || (currentLine - startLine + 1) >= MAX_LINES;
    }
}
