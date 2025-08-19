package it.raptor_service.service.clustering;


import it.raptor_service.config.RaptorProperties;
import it.raptor_service.model.GlobalCluster;
import it.raptor_service.model.TextEmbedding;
import it.raptor_service.service.conversion.WekaConverter;
import it.raptor_service.service.factory.ClusterFactory;
import it.raptor_service.service.optimization.ClusterOptimizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import weka.clusterers.EM;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GlobalClusteringService {

    private final RaptorProperties properties;
    private final WekaConverter wekaConverter;
    private final ClusterOptimizer optimizer;
    private final ClusterFactory clusterFactory;

    public GlobalClusteringService(
            RaptorProperties properties,
            WekaConverter wekaConverter,
            ClusterOptimizer optimizer,
            ClusterFactory clusterFactory) {
        this.properties = properties;
        this.wekaConverter = wekaConverter;
        this.optimizer = optimizer;
        this.clusterFactory = clusterFactory;
    }

    public List<GlobalCluster> performGlobalClustering(List<TextEmbedding> embeddings) throws Exception {
        log.debug("Starting global clustering for {} embeddings", embeddings.size());

        Instances data = wekaConverter.convertToWekaInstances(embeddings);
        EM clusterer = createGlobalClusterer(data);

        return assignToGlobalClusters(embeddings, data, clusterer);
    }

    private EM createGlobalClusterer(Instances data) throws Exception {
        EM clusterer = new EM();
        clusterer.setMaxIterations(properties.getClustering().getMaxIterations());
        clusterer.setSeed(properties.getClustering().getSeed());
        clusterer.setMinLogLikelihoodImprovementIterating(1e-6);

        int maxClusters = Math.min(
                properties.getClustering().getMaxClusters(),
                data.numInstances() / 2
        );
        int optimalClusters = optimizer.findOptimalClusterCount(data, maxClusters);

        clusterer.setNumClusters(optimalClusters);
        clusterer.buildClusterer(data);

        log.debug("Built global clusterer with {} clusters", optimalClusters);
        return clusterer;
    }

    private List<GlobalCluster> assignToGlobalClusters(
            List<TextEmbedding> embeddings,
            Instances data,
            EM clusterer) throws Exception {

        Map<Integer, List<TextEmbedding>> clusterMap = new HashMap<>();
        double threshold = properties.getClustering().getClusterThreshold();

        for (int i = 0; i < embeddings.size(); i++) {
            Instance instance = data.instance(i);
            int clusterAssignment = assignToCluster(instance, clusterer, threshold);

            clusterMap.computeIfAbsent(clusterAssignment, k -> new ArrayList<>())
                    .add(embeddings.get(i));
        }

        List<GlobalCluster> globalClusters = clusterMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> clusterFactory.createGlobalCluster(entry.getKey(), entry.getValue()))
                .collect(Collectors.toList());

        log.debug("Created {} global clusters", globalClusters.size());
        return globalClusters;
    }

    private int assignToCluster(Instance instance, EM clusterer, double threshold) throws Exception {
        double[] probabilities = clusterer.distributionForInstance(instance);

        // Find cluster with maximum probability
        int bestCluster = 0;
        double maxProb = probabilities[0];

        for (int j = 1; j < probabilities.length; j++) {
            if (probabilities[j] > maxProb) {
                maxProb = probabilities[j];
                bestCluster = j;
            }
        }

        // If confidence is too low, assign to "uncertain" cluster
        if (maxProb < threshold) {
            return probabilities.length; // Use next available cluster ID for uncertain assignments
        }

        return bestCluster;
    }
}