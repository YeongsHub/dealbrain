package com.example.sales.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TextChunkingService {

    @Value("${rag.chunk.size:800}")
    private int chunkSize;

    @Value("${rag.chunk.overlap:100}")
    private int chunkOverlap;

    public List<ChunkInfo> chunkText(String text, String documentName) {
        List<ChunkInfo> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        String cleanedText = cleanText(text);

        if (cleanedText.isBlank()) {
            return chunks;
        }

        int start = 0;
        int chunkIndex = 0;
        int previousStart = -1;

        while (start < cleanedText.length()) {
            // Prevent infinite loop
            if (start == previousStart) {
                break;
            }
            previousStart = start;

            int end = Math.min(start + chunkSize, cleanedText.length());

            // Try to break at sentence boundary if not at end
            if (end < cleanedText.length()) {
                end = findSentenceBoundary(cleanedText, start, end);
            }

            String chunkContent = cleanedText.substring(start, end).trim();

            if (!chunkContent.isBlank()) {
                chunks.add(ChunkInfo.builder()
                        .content(chunkContent)
                        .chunkIndex(chunkIndex)
                        .startOffset(start)
                        .endOffset(end)
                        .tokenCount(estimateTokenCount(chunkContent))
                        .build());
                chunkIndex++;
            }

            // If we're at the end, break
            if (end >= cleanedText.length()) {
                break;
            }

            // Move start with overlap, but ensure we make progress
            int newStart = end - chunkOverlap;
            start = Math.max(newStart, start + 1);
        }

        log.info("Created {} chunks from document: {}", chunks.size(), documentName);
        return chunks;
    }

    private String cleanText(String text) {
        return text
                .replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .replaceAll("[ \\t]+", " ")
                .trim();
    }

    private int findSentenceBoundary(String text, int start, int end) {
        int searchStart = Math.max(start + chunkSize / 2, start);
        for (int i = end; i > searchStart; i--) {
            char c = text.charAt(i - 1);
            if (c == '.' || c == '!' || c == '?' || c == '\n') {
                return i;
            }
        }
        // No sentence boundary found, try word boundary
        for (int i = end; i > searchStart; i--) {
            if (Character.isWhitespace(text.charAt(i - 1))) {
                return i;
            }
        }
        return end;
    }

    private int estimateTokenCount(String text) {
        // Rough estimation: ~4 characters per token for English
        return (int) Math.ceil(text.length() / 4.0);
    }

    @Data
    @Builder
    @AllArgsConstructor
    public static class ChunkInfo {
        private String content;
        private int chunkIndex;
        private int startOffset;
        private int endOffset;
        private int tokenCount;
    }
}
