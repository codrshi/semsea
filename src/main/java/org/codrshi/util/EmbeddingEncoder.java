package org.codrshi.util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;
import java.util.List;

// TODO: encode embeddings before sending to chromaDB to reduce bandwidth
public class EmbeddingEncoder {
    public static String[] encode(List<List<Double>> embeddings) {
        String[] result = new String[embeddings.size()];
        ByteBuffer buffer = ByteBuffer.allocate(embeddings.size()*4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        for(int i=0;i<embeddings.size();i++){
            for(Double e:embeddings.get(i)){
                buffer.putFloat(e.floatValue());
            }

            result[i] = Base64.getEncoder().encodeToString(buffer.array());
            buffer.clear();
        }

        return result;
    }
}
