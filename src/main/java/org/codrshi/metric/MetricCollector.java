package org.codrshi.metric;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class MetricCollector {
    private static final Logger log = LogManager.getFormatterLogger(MetricCollector.class.getName());

    private static final EnumMap<MetricType, MetricStat> metrics = new EnumMap<>(MetricType.class);

    static {
        for(MetricType metricType : MetricType.values()) {
            metrics.put(metricType, new MetricStat());
        }
    }

    public static void record(MetricType metricType, long durationNanos) {
        metrics.get(metricType).record(durationNanos);
    }

    public static void print(String title) {
        log.debug("%-35s %-12s %-10s %-10s %-10s %s", title, "totalCalls", "totalTime", "avgTime", "maxTime", "p95");

        for(Map.Entry<MetricType, MetricStat> metric:  metrics.entrySet()) {
            List<Long> durations = metric.getValue().getTotalNanos().stream().sorted().toList();

            if(durations.isEmpty()) {
                continue;
            }

            int totalCalls = durations.size();
            int p95Index = (int) Math.ceil((95.0 / 100.0) * (totalCalls - 1));

            long p95 =  durations.get(p95Index) / (long) 1e6;
            long totalTime = durations.stream().reduce(0L, Long::sum)/ (long) 1e9 ;
            long maxTime = durations.get(totalCalls - 1) / (long) 1e9 ;
            long avgTime = totalTime / totalCalls ;

            log.debug("%-35s %-12s %-10s %-10s %-10s %s",
                    metric.getKey(),
                    totalCalls,
                    totalTime+"s",
                    avgTime+"s",
                    maxTime+"s",
                    p95+"ms");        }
    }
}
