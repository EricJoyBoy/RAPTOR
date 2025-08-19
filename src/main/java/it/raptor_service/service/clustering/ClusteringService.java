package it.raptor_service.service.clustering;



import it.raptor_service.config.RaptorProperties;
import it.raptor_service.model.Cluster;
import it.raptor_service.model.TextEmbedding;
import it.raptor_service.service.clustering.GlobalClusteringService;
import it.raptor_service.service.clustering.LocalClusteringService;
import it.raptor_service.service.factory.ClusterFactory;
import it.raptor_service.service.postprocessing.ClusterPostProcessor;
import it.raptor_service.service.similarity.SimilarityCalculator;

import it.raptor_service.service.validator.embedding.EmbeddingValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
public class ClusteringService {

    private final RaptorProperties properties;
    private final EmbeddingValidator validator;
    private final SimilarityCalculator similarityCalculator;
    private final ClusterFactory clusterFactory;
    private final GlobalClusteringService globalClusteringService;
    private final LocalClusteringService localClusteringService;
    private final ClusterPostProcessor postProcessor;

    public ClusteringService(
            RaptorProperties properties,
            EmbeddingValidator validator,
            SimilarityCalculator similarityCalculator,
            ClusterFactory clusterFactory,
            GlobalClusteringService globalClusteringService,
            LocalClusteringService localClusteringService,
            ClusterPostProcessor postProcessor) {
        this.properties = properties;
        this.validator = validator;
        this.similarityCalculator = similarityCalculator;
        this.clusterFactory = clusterFactory;
        this.globalClusteringService = globalClusteringService;
        this.localClusteringService = localClusteringService;
        this.postProcessor = postProcessor;
    }


    public List<Cluster> performClustering(List<TextEmbedding> embeddings) {
        // Validation
        validator.validateClusteringInput(embeddings);

        if (embeddings.isEmpty()) {
            log.info("Empty embeddings list provided");
            return Collections.emptyList();
        }

        // Handle edge cases
        if (embeddings.size() == 1) {
            return Collections.singletonList(clusterFactory.createSingleCluster(embeddings));
        }

        if (embeddings.size() == 2) {
            return handleTwoEmbeddings(embeddings);
        }

        try {
            log.info("Starting hierarchical clustering for {} embeddings", embeddings.size());

            // Hierarchical clustering process
            var globalClusters = globalClusteringService.performGlobalClustering(embeddings);
            var allClusters = localClusteringService.performLocalClustering(globalClusters);
            var finalClusters = postProcessor.postProcessClusters(allClusters);

            log.info("Clustering completed: {} final clusters", finalClusters.size());
            return finalClusters;

        } catch (Exception e) {
            log.error("Clustering failed, falling back to single cluster", e);
            return Collections.singletonList(clusterFactory.createSingleCluster(embeddings));
        }
    }

    private List<Cluster> handleTwoEmbeddings(List<TextEmbedding> embeddings) {
        double similarity = similarityCalculator.calculateCosineSimilarity(
                embeddings.get(0).getEmbedding(),
                embeddings.get(1).getEmbedding()
        );

        if (similarity > properties.getClustering().getClusterThreshold()) {
            return Collections.singletonList(clusterFactory.createSingleCluster(embeddings));
        } else {
            return clusterFactory.createTwoSeparateClusters(embeddings);
        }
    }
}