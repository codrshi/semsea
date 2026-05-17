package org.codrshi.util;

import java.util.List;
import java.util.stream.Collectors;

public class EmbeddingMapper {
    public static List<List<Float>> map(List<List<Double>> embeddings){
        return embeddings.stream()
                .map(innerList -> innerList.stream()
                        .map(Double::floatValue)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
}
