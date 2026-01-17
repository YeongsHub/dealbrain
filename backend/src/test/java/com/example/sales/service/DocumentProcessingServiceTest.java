package com.example.sales.service;

import com.example.sales.model.entity.Document;
import com.example.sales.model.entity.DocumentChunk;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.DocumentType;
import com.example.sales.model.enums.ProcessingStatus;
import com.example.sales.repository.DealRepository;
import com.example.sales.repository.DocumentChunkRepository;
import com.example.sales.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentProcessingServiceTest {

    @Mock
    private PdfExtractionService pdfExtractionService;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private DealRepository dealRepository;

    private DocumentProcessingService documentProcessingService;
    private User testUser;

    @BeforeEach
    void setUp() {
        documentProcessingService = new DocumentProcessingService(
                pdfExtractionService,
                textChunkingService,
                embeddingService,
                documentRepository,
                documentChunkRepository,
                dealRepository
        );

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .name("Test User")
                .build();
    }

    @Nested
    @DisplayName("Create Document")
    class CreateDocument {

        @Test
        @DisplayName("Should create document with correct properties")
        void shouldCreateDocumentWithCorrectProperties() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "meeting_minutes.pdf",
                    "application/pdf",
                    "PDF content".getBytes()
            );

            when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            Document result = documentProcessingService.createDocument(file, testUser, null);

            assertThat(result).isNotNull();
            assertThat(result.getOriginalFileName()).isEqualTo("meeting_minutes.pdf");
            assertThat(result.getContentType()).isEqualTo("application/pdf");
            assertThat(result.getFileSize()).isEqualTo("PDF content".getBytes().length);
            assertThat(result.getProcessingStatus()).isEqualTo(ProcessingStatus.PENDING);
            assertThat(result.getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should generate unique file name")
        void shouldGenerateUniqueFileName() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "test.pdf",
                    "application/pdf",
                    "content".getBytes()
            );

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(documentCaptor.capture())).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            documentProcessingService.createDocument(file, testUser, null);

            Document savedDocument = documentCaptor.getValue();
            assertThat(savedDocument.getFileName()).isNotEqualTo("test.pdf");
            assertThat(savedDocument.getFileName()).endsWith(".pdf");
        }
    }

    @Nested
    @DisplayName("Infer Document Type")
    class InferDocumentType {

        @Test
        @DisplayName("Should infer MEETING_MINUTES from filename")
        void shouldInferMeetingMinutesFromFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "samsung_meeting_minutes.pdf",
                    "application/pdf",
                    "content".getBytes()
            );

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(documentCaptor.capture())).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            documentProcessingService.createDocument(file, testUser, null);

            assertThat(documentCaptor.getValue().getDocumentType()).isEqualTo(DocumentType.MEETING_MINUTES);
        }

        @Test
        @DisplayName("Should infer PROPOSAL from filename")
        void shouldInferProposalFromFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "sales_proposal_v2.pdf",
                    "application/pdf",
                    "content".getBytes()
            );

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(documentCaptor.capture())).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            documentProcessingService.createDocument(file, testUser, null);

            assertThat(documentCaptor.getValue().getDocumentType()).isEqualTo(DocumentType.PROPOSAL);
        }

        @Test
        @DisplayName("Should infer QUOTATION from filename")
        void shouldInferQuotationFromFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "price_quotation.pdf",
                    "application/pdf",
                    "content".getBytes()
            );

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(documentCaptor.capture())).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            documentProcessingService.createDocument(file, testUser, null);

            assertThat(documentCaptor.getValue().getDocumentType()).isEqualTo(DocumentType.QUOTATION);
        }

        @Test
        @DisplayName("Should default to OTHER for unknown filename")
        void shouldDefaultToOtherForUnknownFilename() {
            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "random_document.pdf",
                    "application/pdf",
                    "content".getBytes()
            );

            ArgumentCaptor<Document> documentCaptor = ArgumentCaptor.forClass(Document.class);
            when(documentRepository.save(documentCaptor.capture())).thenAnswer(invocation -> {
                Document doc = invocation.getArgument(0);
                doc.setId(1L);
                return doc;
            });

            documentProcessingService.createDocument(file, testUser, null);

            assertThat(documentCaptor.getValue().getDocumentType()).isEqualTo(DocumentType.OTHER);
        }
    }

    @Nested
    @DisplayName("Save Chunks")
    class SaveChunks {

        @Test
        @DisplayName("Should save chunks with correct properties")
        void shouldSaveChunksWithCorrectProperties() {
            Document document = Document.builder()
                    .id(1L)
                    .originalFileName("test.pdf")
                    .build();

            List<TextChunkingService.ChunkInfo> chunkInfos = List.of(
                    TextChunkingService.ChunkInfo.builder()
                            .content("Chunk 1 content")
                            .chunkIndex(0)
                            .startOffset(0)
                            .endOffset(15)
                            .tokenCount(4)
                            .build(),
                    TextChunkingService.ChunkInfo.builder()
                            .content("Chunk 2 content")
                            .chunkIndex(1)
                            .startOffset(10)
                            .endOffset(25)
                            .tokenCount(4)
                            .build()
            );

            when(documentChunkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            List<DocumentChunk> result = documentProcessingService.saveChunks(document, chunkInfos, testUser);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getChunkIndex()).isEqualTo(0);
            assertThat(result.get(1).getChunkIndex()).isEqualTo(1);
            assertThat(result.get(0).getDocument()).isEqualTo(document);
            assertThat(result.get(0).getUser()).isEqualTo(testUser);
        }

        @Test
        @DisplayName("Should generate UUID for chunk IDs")
        void shouldGenerateUuidForChunkIds() {
            Document document = Document.builder()
                    .id(1L)
                    .originalFileName("test.pdf")
                    .build();

            List<TextChunkingService.ChunkInfo> chunkInfos = List.of(
                    TextChunkingService.ChunkInfo.builder()
                            .content("Content")
                            .chunkIndex(0)
                            .startOffset(0)
                            .endOffset(7)
                            .tokenCount(1)
                            .build()
            );

            when(documentChunkRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

            List<DocumentChunk> result = documentProcessingService.saveChunks(document, chunkInfos, testUser);

            assertThat(result.get(0).getId()).isNotNull();
            assertThat(result.get(0).getId()).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        }
    }
}
