package it.raptor_service.service.clustering;


import it.raptor_service.config.RaptorProperties;
import it.raptor_service.model.Cluster;
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Slf4j
@Service
public class LocalClusteringService {

    private final RaptorProperties properties;
    private final WekaConverter wekaConverter;
    private final ClusterOptimizer optimizer;
    private final ClusterFactory clusterFactory;
    private final ExecutorService executorService;

    public LocalClusteringService(
            RaptorProperties properties,
            WekaConverter wekaConverter,
            ClusterOptimizer optimizer,
            ClusterFactory clusterFactory) {
        this.properties = properties;
        this.wekaConverter = wekaConverter;
        this.optimizer = optimizer;
        this.clusterFactory = clusterFactory;
        this.executorService = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    public List<Cluster> performLocalClustering(List<GlobalCluster> globalClusters) {
        log.debug("Starting local clustering for {} global clusters", globalClusters.size());

        if (globalClusters.size() == 1) {
            return performSequentialLocalClustering(globalClusters);
        } else {
            return performParallelLocalClustering(globalClusters);
        }
    }

    private List<Cluster> performSequentialLocalClustering(List<GlobalCluster> globalClusters) {
        List<Cluster> allClusters = new ArrayList<>();
        int clusterIdCounter = 0;

        for (GlobalCluster globalCluster : globalClusters) {
            List<Cluster> localClusters = processGlobalCluster(globalCluster, clusterIdCounter);
            allClusters.addAll(localClusters);
            clusterIdCounter += localClusters.size();
        }

        return allClusters;
    }

    private List<Cluster> performParallelLocalClustering(List<GlobalCluster> globalClusters) {
        List<CompletableFuture<List<Cluster>>> futures = new ArrayList<>();
        int clusterIdCounter = 0;

        for (GlobalCluster globalCluster : globalClusters) {
            final int startId = clusterIdCounter;

            CompletableFuture<List<Cluster>> future = CompletableFuture.supplyAsync(() ->
                    processGlobalCluster(globalCluster, startId), executorService);

            futures.add(future);
            clusterIdCounter += estimateClusterCount(globalCluster);
        }

        return collectResults(futures);
    }

    private int estimateClusterCount(GlobalCluster globalCluster) {
        return Math.max(1, globalCluster.getEmbeddings().size() / 10);
    }

    private List<Cluster> collectResults(List<CompletableFuture<List<Cluster>>> futures) {
        List<Cluster> allClusters = new ArrayList<>();

        for (CompletableFuture<List<Cluster>> future : futures) {
            try {
                allClusters.addAll(future.get());
            } catch (Exception e) {
                log.warn("Local clustering task failed", e);
            }
        }

        return allClusters;
    }

    private List<Cluster> processGlobalCluster(GlobalCluster globalCluster, int startId) {
        List<TextEmbedding> embeddings = globalCluster.getEmbeddings();

        if (embeddings.size() <= 3) {
            return Collections.singletonList(
                    clusterFactory.createCluster(startId, embeddings)
            );
        }

        try {
            return performActualLocalClustering(embeddings, startId);
        } catch (Exception e) {
            log.debug("Local clustering failed for {} embeddings: {}",
                    embeddings.size(), e.getMessage());
            return Collections.singletonList(
                    clusterFactory.createCluster(startId, embeddings)
            );
        }
    }

    private List<Cluster> performActualLocalClustering(
            List<TextEmbedding> embeddings,
            int startId) throws Exception {

        Instances data = wekaConverter.convertToWekaInstances(embeddings);
        EM clusterer = createLocalClusterer(data);

        return createLocalClusters(embeddings, data, clusterer, startId);
    }

    private EM createLocalClusterer(Instances data) throws Exception {
        EM clusterer = new EM();
        clusterer.setMaxIterations(properties.getClustering().getMaxIterations() / 2);
        clusterer.setSeed(properties.getClustering().getSeed());

        int maxLocalClusters = Math.min(
                properties.getClustering().getMaxClusters(),
                data.numInstances() / 3
        );

        int optimalClusters = optimizer.findOptimalClusterCount(data, maxLocalClusters);
        clusterer.setNumClusters(optimalClusters);
        clusterer.buildClusterer(data);

        return clusterer;
    }

    private List<Cluster> createLocalClusters(
            List<TextEmbedding> embeddings,
            Instances data,
            EM clusterer,
            int startId) throws Exception {

        Map<Integer, List<TextEmbedding>> localClusterMap = new HashMap<>();

        for (int i = 0; i < embeddings.size(); i++) {
            Instance instance = data.instance(i);
            int clusterAssignment = clusterer.clusterInstance(instance);

            localClusterMap.computeIfAbsent(clusterAssignment, k -> new ArrayList<>())
                    .add(embeddings.get(i));
        }

        return localClusterMap.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> clusterFactory.createCluster(
                        startId + entry.getKey(),
                        entry.getValue()
                ))
                .collect(Collectors.toList());
    }

    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
    }
}