package it.raptor_service.service;

import it.raptor_service.model.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class RaptorService {

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final ClusteringService clusteringService;
    private final TextSplitterService textSplitterService;

    private static final int DEFAULT_CHUNK_SIZE = 2000;
    private static final int DEFAULT_MAX_LEVELS = 3;
    private static final String SUMMARY_TEMPLATE = """
        Here is a subset of documentation that needs to be summarized.

        The documentation provides detailed information about a specific topic.

        Give a detailed summary of the documentation provided, maintaining key concepts and important details.

        Documentation:
        {context}

        Summary:
        """;

    public RaptorService(ChatClient chatClient,
                         EmbeddingModel embeddingModel,
                         ClusteringService clusteringService,
                         TextSplitterService textSplitterService) {
        this.chatClient = chatClient;
        this.embeddingModel = embeddingModel;
        this.clusteringService = clusteringService;
        this.textSplitterService = textSplitterService;
    }

    public RaptorResult processText(String text, int chunkSize, int maxLevels) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("Text cannot be null or empty");
        }

        List<String> chunks = textSplitterService.splitText(text, chunkSize);
        Map<Integer, LevelResult> results = recursiveProcess(chunks, 1, maxLevels);
        List<String> allTexts = collectAllTexts(chunks, results);
        return new RaptorResult(results, allTexts);
    }

    public RaptorResult processText(String text) {
        return processText(text, DEFAULT_CHUNK_SIZE, DEFAULT_MAX_LEVELS);
    }

    private Map<Integer, LevelResult> recursiveProcess(List<String> texts, int level, int maxLevels) {
        Map<Integer, LevelResult> results = new HashMap<>();

        LevelResult currentResult = embedClusterSummarize(texts, level);
        results.put(level, currentResult);

        System.out.println("Level " + level + " generated " +
                currentResult.getSummaries().size() + " clusters");

        if (level < maxLevels && currentResult.getSummaries().size() > 1) {
            List<String> summaries = currentResult.getSummaries().stream()
                    .map(ClusterSummary::getSummary)
                    .collect(Collectors.toList());

            results.putAll(recursiveProcess(summaries, level + 1, maxLevels));
        }

        return results;
    }

    private LevelResult embedClusterSummarize(List<String> texts, int level) {
        List<TextEmbedding> embeddings = generateEmbeddings(texts);
        List<Cluster> clusters = clusteringService.performClustering(embeddings);
        List<ClusterSummary> summaries = generateSummaries(clusters, level);
        return new LevelResult(level, embeddings, clusters, summaries);
    }

    private List<TextEmbedding> generateEmbeddings(List<String> texts) {
        // Uso semplificato senza opzioni personalizzate per evitare problemi di compatibilità
        EmbeddingRequest request = new EmbeddingRequest(texts, null);
        EmbeddingResponse response = embeddingModel.call(request);

        return IntStream.range(0, texts.size())
                .mapToObj(i -> {
                    float[] vector = response.getResults().get(i).getOutput();
                    return new TextEmbedding(i, texts.get(i), vector);
                })
                .collect(Collectors.toList());
    }

    private List<ClusterSummary> generateSummaries(List<Cluster> clusters, int level) {
        PromptTemplate promptTemplate = new PromptTemplate(SUMMARY_TEMPLATE);

        return clusters.stream()
                .map(cluster -> {
                    String context = String.join("\n--- --- \n --- --- \n", cluster.getTexts());
                    String prompt = promptTemplate.render(Map.of("context", context));


                    String summary = chatClient.prompt(prompt).call().content();
                    return new ClusterSummary(cluster.getId(), level, summary, cluster.getTextIds());
                })
                .collect(Collectors.toList());
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