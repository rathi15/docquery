package com.khushirathi.docquery;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestAiConfig {

    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        return mock(EmbeddingModel.class);
    }

    @Bean
    @Primary
    public ChatClient.Builder chatClientBuilder() {
        return mock(ChatClient.Builder.class, org.mockito.Mockito.RETURNS_DEEP_STUBS);
    }
}