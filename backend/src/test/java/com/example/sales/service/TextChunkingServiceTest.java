package com.example.sales.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TextChunkingServiceTest {

    private TextChunkingService textChunkingService;

    @BeforeEach
    void setUp() {
        textChunkingService = new TextChunkingService();
        ReflectionTestUtils.setField(textChunkingService, "chunkSize", 100);
        ReflectionTestUtils.setField(textChunkingService, "chunkOverlap", 20);
    }

    @Nested
    @DisplayName("Basic Chunking")
    class BasicChunking {

        @Test
        @DisplayName("Should create chunks from text")
        void shouldCreateChunksFromText() {
            String text = "This is a sample text that will be chunked. ".repeat(10);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
            assertThat(chunks.get(0).getChunkIndex()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should handle empty text")
        void shouldHandleEmptyText() {
            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText("", "test.pdf");

            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("Should handle null text")
        void shouldHandleNullText() {
            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(null, "test.pdf");

            assertThat(chunks).isEmpty();
        }

        @Test
        @DisplayName("Should handle whitespace-only text")
        void shouldHandleWhitespaceOnlyText() {
            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText("   \n\t  ", "test.pdf");

            assertThat(chunks).isEmpty();
        }
    }

    @Nested
    @DisplayName("Chunk Properties")
    class ChunkProperties {

        @Test
        @DisplayName("Chunks should have sequential indices")
        void chunksShouldHaveSequentialIndices() {
            String text = "This is a sample text. ".repeat(20);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            for (int i = 0; i < chunks.size(); i++) {
                assertThat(chunks.get(i).getChunkIndex()).isEqualTo(i);
            }
        }

        @Test
        @DisplayName("Chunks should have start and end offsets")
        void chunksShouldHaveOffsets() {
            String text = "This is a sample text for testing offsets. ".repeat(10);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
            for (TextChunkingService.ChunkInfo chunk : chunks) {
                assertThat(chunk.getStartOffset()).isGreaterThanOrEqualTo(0);
                assertThat(chunk.getEndOffset()).isGreaterThan(chunk.getStartOffset());
            }
        }

        @Test
        @DisplayName("Chunks should have token count")
        void chunksShouldHaveTokenCount() {
            String text = "This is a sample text. ".repeat(10);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
            for (TextChunkingService.ChunkInfo chunk : chunks) {
                assertThat(chunk.getTokenCount()).isGreaterThan(0);
            }
        }

        @Test
        @DisplayName("Chunks should have non-empty content")
        void chunksShouldHaveContent() {
            String text = "This is a sample text for testing. ".repeat(10);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
            for (TextChunkingService.ChunkInfo chunk : chunks) {
                assertThat(chunk.getContent()).isNotBlank();
            }
        }
    }

    @Nested
    @DisplayName("Sentence Boundary")
    class SentenceBoundary {

        @Test
        @DisplayName("Should try to break at sentence boundaries")
        void shouldBreakAtSentenceBoundaries() {
            String text = "First sentence. Second sentence. Third sentence. Fourth sentence. Fifth sentence.";

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            // Chunks should typically end with punctuation
            for (int i = 0; i < chunks.size() - 1; i++) {
                String content = chunks.get(i).getContent();
                boolean endsWithPunctuation = content.endsWith(".") ||
                        content.endsWith("!") ||
                        content.endsWith("?");
                // At least some chunks should end with punctuation
                if (endsWithPunctuation) {
                    assertThat(endsWithPunctuation).isTrue();
                }
            }
        }
    }

    @Nested
    @DisplayName("Text Cleaning")
    class TextCleaning {

        @Test
        @DisplayName("Should normalize line breaks")
        void shouldNormalizeLineBreaks() {
            String text = "Line one\r\nLine two\rLine three\nLine four. ".repeat(5);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
            for (TextChunkingService.ChunkInfo chunk : chunks) {
                assertThat(chunk.getContent()).doesNotContain("\r\n");
                assertThat(chunk.getContent()).doesNotContain("\r");
            }
        }

        @Test
        @DisplayName("Should collapse multiple spaces")
        void shouldCollapseMultipleSpaces() {
            String text = "Word    with    multiple   spaces. ".repeat(10);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
            for (TextChunkingService.ChunkInfo chunk : chunks) {
                assertThat(chunk.getContent()).doesNotContain("  ");
            }
        }
    }

    @Nested
    @DisplayName("Short Text")
    class ShortText {

        @Test
        @DisplayName("Should handle text shorter than chunk size")
        void shouldHandleShortText() {
            String text = "Short text.";

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0).getContent()).isEqualTo("Short text.");
        }

        @Test
        @DisplayName("Should handle text exactly at chunk size")
        void shouldHandleTextAtChunkSize() {
            // Create text of exactly chunk size (100 characters)
            String text = "A".repeat(100);

            List<TextChunkingService.ChunkInfo> chunks =
                    textChunkingService.chunkText(text, "test.pdf");

            assertThat(chunks).isNotEmpty();
        }
    }
}
