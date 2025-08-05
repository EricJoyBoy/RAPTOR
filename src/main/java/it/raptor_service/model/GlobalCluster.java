package it.raptor_service.model;

import java.util.List;

// Global cluster for hierarchical clustering
public class GlobalCluster {
    private final int id;
    private final List<TextEmbedding> embeddings;

    public GlobalCluster(int id, List<TextEmbedding> embeddings) {
        this.id = id;
        this.embeddings = embeddings;
    }

    public int getId() { return id; }
    public List<TextEmbedding> getEmbeddings() { return embeddings; }
}