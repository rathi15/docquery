package com.khushirathi.docquery.ingestion;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.stereotype.Component;

@Component
public class TextChunker {
    
    public List<Document> chunk(String documentText){
        Document source = new Document(documentText);
        TokenTextSplitter splitter = new TokenTextSplitter();
        return splitter.split(source); 
    }
}
