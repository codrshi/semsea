package org.codrshi.util;

import java.sql.Timestamp;

public record WorkspaceDetails(String location, Timestamp lastRefresh) {
}
