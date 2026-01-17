package com.example.sales.service;

import com.example.sales.model.dto.ChatQueryRequest;
import com.example.sales.model.dto.ChatQueryResponse;
import com.example.sales.model.dto.EvidenceItem;
import com.example.sales.model.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient.Builder chatClientBuilder;

    @Value("${rag.similarity.threshold:0.75}")
    private double similarityThreshold;

    @Value("${rag.top-k:5}")
    private int defaultTopK;

    private static final String SYSTEM_PROMPT = """
        You are an AI Sales Assistant for the AI Sales Brain platform. Your role is to answer
        questions about sales deals based on the provided document context.

        Guidelines:
        1. Answer ONLY based on the provided context documents
        2. If the context doesn't contain relevant information, say so clearly
        3. Always cite your sources by mentioning the document name
        4. Be concise but thorough
        5. Focus on actionable sales insights

        Context Documents:
        %s
        """;

    public ChatQueryResponse query(ChatQueryRequest request, User user) {
        int topK = request.getTopK() != null ? request.getTopK() : defaultTopK;

        // Build filter for user-scoped search
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        var filterExpression = builder.eq("userId", user.getId().toString()).build();

        // Perform similarity search
        SearchRequest searchRequest = SearchRequest.query(request.getQuery())
                .withTopK(topK)
                .withSimilarityThreshold(similarityThreshold)
                .withFilterExpression(filterExpression);

        List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);

        if (relevantDocs.isEmpty()) {
            return buildNoResultsResponse(request.getQuery());
        }

        // Build context from relevant documents
        String context = buildContext(relevantDocs);

        // Generate answer using chat model
        String answer = generateAnswer(request.getQuery(), context);

        // Build evidence list
        List<EvidenceItem> evidence = buildEvidence(relevantDocs);

        // Determine confidence based on relevance scores
        String confidence = determineConfidence(relevantDocs);

        return ChatQueryResponse.builder()
                .query(request.getQuery())
                .answer(answer)
                .evidence(evidence)
                .confidence(confidence)
                .build();
    }

    private String buildContext(List<Document> documents) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            Object fileNameObj = doc.getMetadata().get("fileName");
            String fileName = fileNameObj != null ? fileNameObj.toString() : "Unknown";
            context.append(String.format("[Document %d: %s]\n%s\n\n",
                    i + 1, fileName, doc.getContent()));
        }
        return context.toString();
    }

    private String generateAnswer(String query, String context) {
        ChatClient chatClient = chatClientBuilder.build();

        String systemPrompt = String.format(SYSTEM_PROMPT, context);

        return chatClient.prompt()
                .system(systemPrompt)
                .user(query)
                .call()
                .content();
    }

    private List<EvidenceItem> buildEvidence(List<Document> documents) {
        List<EvidenceItem> evidence = new ArrayList<>();

        for (Document doc : documents) {
            Object fileNameObj = doc.getMetadata().get("fileName");
            String fileName = fileNameObj != null ? fileNameObj.toString() : "Unknown";

            Object pageNumberObj = doc.getMetadata().get("pageNumber");
            Integer pageNumber = 0;
            if (pageNumberObj instanceof Integer) {
                pageNumber = (Integer) pageNumberObj;
            } else if (pageNumberObj != null) {
                try {
                    pageNumber = Integer.parseInt(pageNumberObj.toString());
                } catch (NumberFormatException e) {
                    pageNumber = 0;
                }
            }

            // Get score from metadata if available
            Object scoreObj = doc.getMetadata().get("distance");
            Double score = 0.0;
            if (scoreObj instanceof Double) {
                score = 1.0 - (Double) scoreObj; // Convert distance to similarity
            } else if (scoreObj instanceof Number) {
                score = 1.0 - ((Number) scoreObj).doubleValue();
            }

            // Truncate excerpt to reasonable length
            String excerpt = doc.getContent();
            if (excerpt != null && excerpt.length() > 300) {
                excerpt = excerpt.substring(0, 297) + "...";
            }

            evidence.add(EvidenceItem.builder()
                    .source(fileName)
                    .excerpt(excerpt)
                    .page(pageNumber)
                    .relevanceScore(Math.round(score * 100.0) / 100.0)
                    .build());
        }

        return evidence;
    }

    private String determineConfidence(List<Document> documents) {
        if (documents.isEmpty()) {
            return "Low";
        }

        // Use a default score based on the fact that documents were returned
        // (they passed the similarity threshold)
        double avgScore = 0.8; // Default confidence for returned documents

        if (documents.size() >= 3) {
            return "High";
        } else if (documents.size() >= 2) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    private ChatQueryResponse buildNoResultsResponse(String query) {
        return ChatQueryResponse.builder()
                .query(query)
                .answer("I couldn't find any relevant information in your uploaded documents to answer this question. Please make sure you have uploaded relevant PDF documents.")
                .evidence(List.of())
                .confidence("Low")
                .build();
    }
}
