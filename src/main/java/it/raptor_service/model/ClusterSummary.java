package it.raptor_service.model;

import java.util.List;

public class ClusterSummary {
    private final int clusterId;
    private final int level;
    private final String summary;
    private final List<Integer> originalTextIds;

    public ClusterSummary(int clusterId, int level, String summary, List<Integer> originalTextIds) {
        this.clusterId = clusterId;
        this.level = level;
        this.summary = summary;
        this.originalTextIds = originalTextIds;
    }

    public int getClusterId() { return clusterId; }
    public int getLevel() { return level; }
    public String getSummary() { return summary; }
    public List<Integer> getOriginalTextIds() { return originalTextIds; }
}