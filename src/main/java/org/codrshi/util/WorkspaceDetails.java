package org.codrshi.util;

import java.sql.Timestamp;

public record WorkspaceDetails(String id, String location, Timestamp lastRefresh) {
}
