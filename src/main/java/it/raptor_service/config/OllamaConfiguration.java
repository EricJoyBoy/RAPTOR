package it.raptor_service.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OllamaConfiguration {

    @Bean
    public OllamaApi ollamaApi() {
        return new OllamaApi();
    }

    @Bean
    public OllamaChatModel ollamaChatModel() {
        return new OllamaChatModel(ollamaApi(),OllamaOptions.builder().build(),null,null,null);
    }

    @Bean
    public ChatClient chatClient() {
        return ChatClient.builder(ollamaChatModel()).build();
    }

    @Bean
    public EmbeddingModel embeddingModel() {
        return new OllamaEmbeddingModel(ollamaApi(),OllamaOptions.builder().build(),null,null);
    }
}