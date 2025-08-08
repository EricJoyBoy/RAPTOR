package it.raptor_service.service;

import it.raptor_service.config.RaptorProperties;
import it.raptor_service.model.*;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@Slf4j
public class RaptorService {




    private final EmbeddingModel embeddingModel;
    private final ClusteringService clusteringService;
    private final TextSplitterService textSplitterService;
    private final RaptorProperties properties;
    private final ChatModel chatModel;

    private static final String SUMMARY_TEMPLATE = """
        Here is a subset of documentation that needs to be summarized.

        The documentation provides detailed information about a specific topic.

        Give a detailed summary of the documentation provided, maintaining key concepts and important details.

        Documentation:
        {context}

        Summary:
        """;

    public RaptorService(ChatModel chatModel,
                         EmbeddingModel embeddingModel,
                         ClusteringService clusteringService,
                         TextSplitterService textSplitterService,
                         RaptorProperties properties) {

        this.embeddingModel = embeddingModel;
        this.clusteringService = clusteringService;
        this.textSplitterService = textSplitterService;
        this.properties = properties;
        this.chatModel = chatModel;
    }

    public RaptorResult processText(String text, int chunkSize, int maxLevels) {
        log.info("Starting RAPTOR processing with chunkSize={}, maxLevels={}", chunkSize, maxLevels);

        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        try {
            long startTime = System.currentTimeMillis();

            // Step 1: Text splitting
            log.debug("Splitting text into chunks...");
            List<String> chunks = textSplitterService.splitText(text, chunkSize);
            log.info("Text split into {} chunks", chunks.size());

            // Step 2: Recursive processing
            Map<Integer, LevelResult> results = recursiveProcess(chunks, 1, maxLevels);

            // Step 3: Collect all texts
            List<String> allTexts = collectAllTexts(chunks, results);

            long processingTime = System.currentTimeMillis() - startTime;
            log.info("RAPTOR processing completed in {}ms with {} levels", processingTime, results.size());

            return new RaptorResult(results, allTexts);

        } catch (Exception e) {
            log.error("Error during RAPTOR processing: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to process text: " + e.getMessage(), e);
        }
    }

    public RaptorResult processText(String text) {
        return processText(text,
                properties.getProcessing().getDefaultChunkSize(),
                properties.getProcessing().getDefaultMaxLevels());
    }

    private Map<Integer, LevelResult> recursiveProcess(List<String> texts, int level, int maxLevels) {
        Map<Integer, LevelResult> results = new HashMap<>();

        log.debug("Processing level {} with {} texts", level, texts.size());

        try {
            LevelResult currentResult = embedClusterSummarize(texts, level);
            results.put(level, currentResult);

            log.info("Level {} generated {} clusters", level, currentResult.getSummaries().size());

            if (level < maxLevels && currentResult.getSummaries().size() > 1) {
                List<String> summaries = currentResult.getSummaries().stream()
                        .map(ClusterSummary::getSummary)
                        .collect(Collectors.toList());

                results.putAll(recursiveProcess(summaries, level + 1, maxLevels));
            }

        } catch (Exception e) {
            log.error("Error processing level {}: {}", level, e.getMessage(), e);
            // Create a fallback result for this level
            LevelResult fallbackResult = createFallbackResult(texts, level);
            results.put(level, fallbackResult);
        }

        return results;
    }

    private LevelResult embedClusterSummarize(List<String> texts, int level) {
        log.debug("Generating embeddings for level {} with {} texts", level, texts.size());
        List<TextEmbedding> embeddings = generateEmbeddings(texts);

        log.debug("Performing clustering for level {}", level);
        List<Cluster> clusters = clusteringService.performClustering(embeddings);

        log.debug("Generating summaries for level {} with {} clusters", level, clusters.size());
        List<ClusterSummary> summaries = generateSummaries(clusters, level);

        return new LevelResult(level, embeddings, clusters, summaries);
    }

    private List<TextEmbedding> generateEmbeddings(List<String> texts) {
        try {
            log.debug("Generating embeddings for {} texts", texts.size());

            EmbeddingRequest request = new EmbeddingRequest(texts, null);
            EmbeddingResponse response = embeddingModel.call(request);

            List<TextEmbedding> embeddings = IntStream.range(0, texts.size())
                    .mapToObj(i -> {
                        float[] vector = response.getResults().get(i).getOutput();
                        return new TextEmbedding(i, texts.get(i), vector);
                    })
                    .collect(Collectors.toList());

            log.debug("Successfully generated {} embeddings", embeddings.size());
            return embeddings;

        } catch (Exception e) {
            log.error("Error generating embeddings: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate embeddings: " + e.getMessage(), e);
        }
    }

    private List<ClusterSummary> generateSummaries(List<Cluster> clusters, int level) {
        log.debug("Generating summaries for {} clusters at level {}", clusters.size(), level);

        PromptTemplate promptTemplate = new PromptTemplate(SUMMARY_TEMPLATE);

        return clusters.stream()
                .map(cluster -> {
                    try {
                        String context = String.join("\n--- --- \n --- --- \n", cluster.getTexts());
                        String prompt = promptTemplate.render(Map.of("context", context));

                        log.debug("Generating summary for cluster {} at level {}", cluster.getId(), level);
                        String summary = chatModel.call(new Prompt(new UserMessage(prompt)))
                                .getResult()
                                .getOutput()
                                .getText();
                        
                        return new ClusterSummary(cluster.getId(), level, summary, cluster.getTextIds());
                        
                    } catch (Exception e) {
                        log.error("Error generating summary for cluster {} at level {}: {}", 
                                cluster.getId(), level, e.getMessage(), e);
                        
                        // Return a fallback summary
                        String fallbackSummary = "Summary generation failed for this cluster.";
                        return new ClusterSummary(cluster.getId(), level, fallbackSummary, cluster.getTextIds());
                    }
                })
                .collect(Collectors.toList());
    }

    private LevelResult createFallbackResult(List<String> texts, int level) {
        log.warn("Creating fallback result for level {} with {} texts", level, texts.size());
        
        // Create empty embeddings (will be null or empty arrays)
        List<TextEmbedding> fallbackEmbeddings = texts.stream()
                .map(text -> new TextEmbedding(0, text, new float[0]))
                .collect(Collectors.toList());
        
        // Create a single cluster with all texts
        List<Cluster> fallbackClusters = List.of(new Cluster(0, texts, 
                IntStream.range(0, texts.size()).boxed().collect(Collectors.toList())));
        
        // Create fallback summaries
        List<ClusterSummary> fallbackSummaries = List.of(new ClusterSummary(0, level, 
                "Processing failed for this level.", List.of(0)));
        
        return new LevelResult(level, fallbackEmbeddings, fallbackClusters, fallbackSummaries);
    }

    private List<String> collectAllTexts(List<String> originalTexts, Map<Integer, LevelResult> results) {
        List<String> allTexts = new ArrayList<>(originalTexts);

        results.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    List<String> summaries = entry.getValue().getSummaries().stream()
                            .map(ClusterSummary::getSummary)
                            .collect(Collectors.toList());
                    allTexts.addAll(summaries);
                });

        return allTexts;
    }
}