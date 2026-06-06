package org.codrshi.metric;

import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class MetricStat {
    private List<Long> totalNanos = new ArrayList<>();

    public void record(long durationNanos) {
        totalNanos.add(durationNanos);
    }
}
