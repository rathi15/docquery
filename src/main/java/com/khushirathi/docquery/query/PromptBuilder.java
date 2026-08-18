package com.khushirathi.docquery.query;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    public String build(String question, List<Document> chunks) {
        String context = chunks.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n---\n\n"));

        return """
                You are a helpful assistant that answers questions using ONLY the context provided below.
                If the answer is not contained in the context, say "I could not find that in your documents."
                Do not use any outside knowledge. Do not make up information.

                Context:
                %s

                Question: %s

                Answer:
                """.formatted(context, question);
    }
}