package it.raptor_service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class HealthConfiguration {

    @Component
    public static class OllamaChatHealthIndicator implements HealthIndicator {
        
        private final ChatClient chatClient;
        
        public OllamaChatHealthIndicator(ChatClient chatClient) {
            this.chatClient = chatClient;
        }
        
        @Override
        public Health health() {
            try {
                // Simple test prompt to check if Ollama chat is responding
                String response = chatClient.prompt("Hello").call().content();
                return Health.up()
                        .withDetail("service", "Ollama Chat")
                        .withDetail("response", "Service responding")
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "Ollama Chat")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }
    
    @Component
    public static class OllamaEmbeddingHealthIndicator implements HealthIndicator {
        
        private final EmbeddingModel embeddingModel;
        
        public OllamaEmbeddingHealthIndicator(EmbeddingModel embeddingModel) {
            this.embeddingModel = embeddingModel;
        }
        
        @Override
        public Health health() {
            try {
                // Simple test embedding to check if Ollama embedding is responding
                var request = new org.springframework.ai.embedding.EmbeddingRequest(List.of("test"), null);
                var response = embeddingModel.call(request);
                
                if (response.getResults().isEmpty()) {
                    return Health.down()
                            .withDetail("service", "Ollama Embedding")
                            .withDetail("error", "No embedding results")
                            .build();
                }
                
                return Health.up()
                        .withDetail("service", "Ollama Embedding")
                        .withDetail("dimensions", response.getResults().get(0).getOutput().length)
                        .build();
            } catch (Exception e) {
                return Health.down()
                        .withDetail("service", "Ollama Embedding")
                        .withDetail("error", e.getMessage())
                        .build();
            }
        }
    }
}
