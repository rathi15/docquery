package com.khushirathi.docquery.query;

import java.util.List;
import java.util.UUID;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Component;


@Component
public class ChunkRetriever {

    private static final int TOP_K = 5;

    private final VectorStore vectorStore;

    public ChunkRetriever(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> retrieve(String question, UUID userId) {
        var filter = new FilterExpressionBuilder()
                .eq("userId", userId.toString())
                .build();

        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(TOP_K)
                .filterExpression(filter)
                .build();

        return vectorStore.similaritySearch(request);
    }
}
