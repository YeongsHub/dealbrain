package com.example.sales.service;

import com.example.sales.exception.EmbeddingGenerationException;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmbeddingService {

    private final VectorStore vectorStore;

    @Retryable(
        retryFor = {Exception.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public void storeChunks(List<ChunkWithMetadata> chunks, Long userId) {
        try {
            List<Document> documents = chunks.stream()
                    .map(chunk -> new Document(
                            chunk.getId(),
                            chunk.getContent(),
                            Map.of(
                                    "userId", userId.toString(),
                                    "documentId", chunk.getDocumentId().toString(),
                                    "fileName", chunk.getFileName(),
                                    "chunkIndex", chunk.getChunkIndex(),
                                    "pageNumber", chunk.getPageNumber() != null ? chunk.getPageNumber() : 0
                            )
                    ))
                    .collect(Collectors.toList());

            vectorStore.add(documents);
            log.info("Stored {} chunks in vector store for user {}", chunks.size(), userId);

        } catch (Exception e) {
            log.error("Failed to store chunks in vector store", e);
            throw new EmbeddingGenerationException("Failed to generate and store embeddings: " + e.getMessage());
        }
    }

    public void deleteChunks(List<String> chunkIds) {
        if (chunkIds == null || chunkIds.isEmpty()) {
            return;
        }
        try {
            vectorStore.delete(chunkIds);
            log.info("Deleted {} chunks from vector store", chunkIds.size());
        } catch (Exception e) {
            log.error("Failed to delete chunks from vector store", e);
        }
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ChunkWithMetadata {
        private String id;
        private String content;
        private Long documentId;
        private String fileName;
        private Integer chunkIndex;
        private Integer pageNumber;
    }
}
