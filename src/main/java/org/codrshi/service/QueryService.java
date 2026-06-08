package org.codrshi.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codrshi.api.ChromaClient;
import org.codrshi.api.OllamaClient;
import org.codrshi.config.ConfigManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QueryService {

    private static final Logger log = LogManager.getLogger(QueryService.class);

    private static final int LIMIT_MULTIPLIER = ConfigManager.getConfig().getLimitMultiplier();

    private final ChromaClient chromaClient;
    private final OllamaClient ollamaClient;

    public QueryService(){
        chromaClient = new ChromaClient();
        ollamaClient = new OllamaClient();
    }

    public List<List<String>> search(String query, int limit){
        String collectionId = ConfigManager.getConfig().getCollectionId();
        log.info("Searching collection '{}' for query=\"{}\" (limit={}, candidatePool={})",
                collectionId, query, limit, limit * LIMIT_MULTIPLIER);

        List<List<Float>> queryEmbedding = ollamaClient.createEmbeddings(List.of(query));
        log.debug("Generated {} query embedding vector(s)", queryEmbedding.size());

        Map<Object, Object> response = chromaClient.searchEmbeddings(collectionId, queryEmbedding, limit * LIMIT_MULTIPLIER);

        List<List<String>> result = new ArrayList<>();
        Map<String, FileScoreStat> fileScoreStatMap = aggregate(response, result);
        log.debug("Aggregated {} unique file(s) from vector store response", fileScoreStatMap.size());

        List<List<String>> ranked = rankFiles(fileScoreStatMap, result, limit);
        log.info("Returning {} ranked result(s) for query=\"{}\"", ranked.size(), query);
        return ranked;
    }

    private Map<String, FileScoreStat> aggregate(Map<Object, Object> response, List<List<String>> result) {
        List<String> documents = ((List<List<String>>) response.get("documents")).getFirst();
        List<Map<String, String>> metadatas = ((List<List<Map<String, String>>>) response.get("metadatas")).getFirst();
        List<Double> distances =  ((List<List<Double>>) response.get("distances")).getFirst();

        Map<String, FileScoreStat> map = new HashMap<>();

        for(int i = 0; i < documents.size(); i++){
            String fileName = documents.get(i);
            FileScoreStat fileScoreStat;

            if(!map.containsKey(fileName)){
                fileScoreStat = new FileScoreStat();
                map.put(fileName, fileScoreStat);
                result.add(List.of(fileName, metadatas.get(i).get("last-modified-time")));
            }
            else{
                fileScoreStat = map.get(fileName);
            }

            double score = 1d/(1d + distances.get(i));
            fileScoreStat.maxScore = Double.max(fileScoreStat.maxScore, score);
            fileScoreStat.totalScore += score;
            fileScoreStat.count++;
        }

        return map;
    }

    private List<List<String>> rankFiles(Map<String, FileScoreStat> map, List<List<String>> result, int limit) {
        return result.stream().sorted((List<String> x, List<String> y) -> {
            String xFileName = x.getFirst(), yFileName = y.getFirst();
            double xScore = map.get(xFileName).maxScore + 0.1 * (map.get(xFileName).totalScore/map.get(xFileName).count);
            double yScore = map.get(yFileName).maxScore + 0.1 * (map.get(yFileName).totalScore/map.get(yFileName).count);

            return Double.compare(yScore, xScore);
        }).limit(limit).toList();
    }

    private static class FileScoreStat {
        double maxScore;
        double totalScore;
        double count;
    }
}
