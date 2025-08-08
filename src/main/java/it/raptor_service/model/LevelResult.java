package it.raptor_service.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LevelResult {
    private int level;
    private List<TextEmbedding> embeddings;
    private List<Cluster> clusters;
    private List<ClusterSummary> summaries;
}