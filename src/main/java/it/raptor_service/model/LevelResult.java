package it.raptor_service.model;

import java.util.List;


public class LevelResult {
    private final int level;
    private final List<TextEmbedding> embeddings;
    private final List<Cluster> clusters;
    private final List<ClusterSummary> summaries;

    public LevelResult(int level, List<TextEmbedding> embeddings,
                       List<Cluster> clusters, List<ClusterSummary> summaries) {
        this.level = level;
        this.embeddings = embeddings;
        this.clusters = clusters;
        this.summaries = summaries;
    }

    public int getLevel() { return level; }
    public List<TextEmbedding> getEmbeddings() { return embeddings; }
    public List<Cluster> getClusters() { return clusters; }
    public List<ClusterSummary> getSummaries() { return summaries; }
}