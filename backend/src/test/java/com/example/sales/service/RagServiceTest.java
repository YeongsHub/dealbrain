package com.example.sales.service;

import com.example.sales.model.dto.ChatQueryRequest;
import com.example.sales.model.dto.ChatQueryResponse;
import com.example.sales.model.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

    @Mock
    private VectorStore vectorStore;

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec chatClientRequestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private RagService ragService;
    private User testUser;

    @BeforeEach
    void setUp() {
        ragService = new RagService(vectorStore, chatClientBuilder);
        ReflectionTestUtils.setField(ragService, "similarityThreshold", 0.75);
        ReflectionTestUtils.setField(ragService, "defaultTopK", 5);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Nested
    @DisplayName("No Results")
    class NoResults {

        @Test
        @DisplayName("Should return no results response when no documents found")
        void shouldReturnNoResultsResponseWhenNoDocumentsFound() {
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("What was discussed in the meeting?")
                    .build();

            ChatQueryResponse response = ragService.query(request, testUser);

            assertThat(response.getConfidence()).isEqualTo("Low");
            assertThat(response.getEvidence()).isEmpty();
            assertThat(response.getAnswer()).contains("couldn't find");
            assertThat(response.getQuery()).isEqualTo("What was discussed in the meeting?");
        }
    }

    @Nested
    @DisplayName("With Results")
    class WithResults {

        @Test
        @DisplayName("Should build evidence from relevant documents")
        void shouldBuildEvidenceFromRelevantDocuments() {
            Document doc = new Document("Meeting content here",
                    Map.of("fileName", "meeting.pdf", "pageNumber", 1, "userId", "1"));

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
            when(chatClientBuilder.build()).thenReturn(chatClient);
            when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Generated answer based on the meeting content.");

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("What was discussed?")
                    .build();

            ChatQueryResponse response = ragService.query(request, testUser);

            assertThat(response).isNotNull();
            assertThat(response.getAnswer()).isEqualTo("Generated answer based on the meeting content.");
            assertThat(response.getEvidence()).hasSize(1);
            assertThat(response.getEvidence().get(0).getSource()).isEqualTo("meeting.pdf");
        }

        @Test
        @DisplayName("Should determine confidence based on document count")
        void shouldDetermineConfidenceBasedOnDocumentCount() {
            Document doc1 = new Document("Content 1", Map.of("fileName", "doc1.pdf", "pageNumber", 1, "userId", "1"));
            Document doc2 = new Document("Content 2", Map.of("fileName", "doc2.pdf", "pageNumber", 1, "userId", "1"));
            Document doc3 = new Document("Content 3", Map.of("fileName", "doc3.pdf", "pageNumber", 1, "userId", "1"));

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2, doc3));
            when(chatClientBuilder.build()).thenReturn(chatClient);
            when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Answer");

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("Question?")
                    .build();

            ChatQueryResponse response = ragService.query(request, testUser);

            assertThat(response.getConfidence()).isEqualTo("High");
        }

        @Test
        @DisplayName("Should truncate long excerpts")
        void shouldTruncateLongExcerpts() {
            String longContent = "A".repeat(500);
            Document doc = new Document(longContent,
                    Map.of("fileName", "doc.pdf", "pageNumber", 1, "userId", "1"));

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
            when(chatClientBuilder.build()).thenReturn(chatClient);
            when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Answer");

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("Question?")
                    .build();

            ChatQueryResponse response = ragService.query(request, testUser);

            assertThat(response.getEvidence()).hasSize(1);
            assertThat(response.getEvidence().get(0).getExcerpt().length()).isLessThanOrEqualTo(300);
            assertThat(response.getEvidence().get(0).getExcerpt()).endsWith("...");
        }
    }

    @Nested
    @DisplayName("Query Parameters")
    class QueryParameters {

        @Test
        @DisplayName("Should use custom topK when provided")
        void shouldUseCustomTopKWhenProvided() {
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("Question?")
                    .topK(10)
                    .build();

            ragService.query(request, testUser);

            verify(vectorStore).similaritySearch(any(SearchRequest.class));
        }

        @Test
        @DisplayName("Should use default topK when not provided")
        void shouldUseDefaultTopKWhenNotProvided() {
            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("Question?")
                    .build();

            ragService.query(request, testUser);

            verify(vectorStore).similaritySearch(any(SearchRequest.class));
        }
    }

    @Nested
    @DisplayName("Confidence Levels")
    class ConfidenceLevels {

        @Test
        @DisplayName("Should return Low confidence for single document")
        void shouldReturnLowConfidenceForSingleDocument() {
            Document doc = new Document("Content", Map.of("fileName", "doc.pdf", "pageNumber", 1, "userId", "1"));

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc));
            when(chatClientBuilder.build()).thenReturn(chatClient);
            when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Answer");

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("Question?")
                    .build();

            ChatQueryResponse response = ragService.query(request, testUser);

            assertThat(response.getConfidence()).isEqualTo("Low");
        }

        @Test
        @DisplayName("Should return Medium confidence for two documents")
        void shouldReturnMediumConfidenceForTwoDocuments() {
            Document doc1 = new Document("Content 1", Map.of("fileName", "doc1.pdf", "pageNumber", 1, "userId", "1"));
            Document doc2 = new Document("Content 2", Map.of("fileName", "doc2.pdf", "pageNumber", 1, "userId", "1"));

            when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(doc1, doc2));
            when(chatClientBuilder.build()).thenReturn(chatClient);
            when(chatClient.prompt()).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.system(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.user(anyString())).thenReturn(chatClientRequestSpec);
            when(chatClientRequestSpec.call()).thenReturn(callResponseSpec);
            when(callResponseSpec.content()).thenReturn("Answer");

            ChatQueryRequest request = ChatQueryRequest.builder()
                    .query("Question?")
                    .build();

            ChatQueryResponse response = ragService.query(request, testUser);

            assertThat(response.getConfidence()).isEqualTo("Medium");
        }
    }
}
