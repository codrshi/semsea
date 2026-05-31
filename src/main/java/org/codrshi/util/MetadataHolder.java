package org.codrshi.util;

import java.util.List;

public record MetadataHolder(List<String> ids, long lastModifiedAt, long fileSize) {
}
