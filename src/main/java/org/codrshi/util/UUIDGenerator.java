package org.codrshi.util;

import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

public class UUIDGenerator {
    public static List<UUID> get(int n){
        return Stream.generate(UUID::randomUUID).limit(n).toList();
    }
}
