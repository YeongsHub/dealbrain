package com.example.sales.service;

import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.Document;
import com.example.sales.model.entity.DocumentChunk;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.DocumentType;
import com.example.sales.model.enums.ProcessingStatus;
import com.example.sales.repository.DealRepository;
import com.example.sales.repository.DocumentChunkRepository;
import com.example.sales.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessingService {

    private final PdfExtractionService pdfExtractionService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DealRepository dealRepository;

    @Transactional
    public Document createDocument(MultipartFile file, User user, String dealId) {
        Deal deal = null;
        if (dealId != null && !dealId.isBlank()) {
            deal = dealRepository.findByDealIdAndUser(dealId, user).orElse(null);
        }

        Document document = Document.builder()
                .fileName(generateUniqueFileName(file.getOriginalFilename()))
                .originalFileName(file.getOriginalFilename())
                .contentType(file.getContentType())
                .fileSize(file.getSize())
                .documentType(inferDocumentType(file.getOriginalFilename()))
                .processingStatus(ProcessingStatus.PENDING)
                .user(user)
                .deal(deal)
                .build();

        return documentRepository.save(document);
    }

    @Async("documentProcessingExecutor")
    public void processDocumentAsync(Long documentId, MultipartFile file, User user) {
        Document document = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found: " + documentId));

        try {
            document.setProcessingStatus(ProcessingStatus.PROCESSING);
            documentRepository.save(document);

            // Step 1: Extract text
            PdfExtractionService.ExtractedContent extracted = pdfExtractionService.extractText(file);
            document.setTotalPages(extracted.getPageCount());

            // Step 2: Chunk text
            List<TextChunkingService.ChunkInfo> chunkInfos =
                    textChunkingService.chunkText(extracted.getText(), document.getOriginalFileName());

            // Step 3: Save chunks to database
            List<DocumentChunk> savedChunks = saveChunks(document, chunkInfos, user);
            document.setTotalChunks(savedChunks.size());

            // Step 4: Generate embeddings and store in vector store
            List<EmbeddingService.ChunkWithMetadata> chunksWithMetadata = savedChunks.stream()
                    .map(chunk -> EmbeddingService.ChunkWithMetadata.builder()
                            .id(chunk.getId())
                            .content(chunk.getContent())
                            .documentId(document.getId())
                            .fileName(document.getOriginalFileName())
                            .chunkIndex(chunk.getChunkIndex())
                            .pageNumber(chunk.getPageNumber())
                            .build())
                    .toList();

            embeddingService.storeChunks(chunksWithMetadata, user.getId());

            document.setProcessingStatus(ProcessingStatus.COMPLETED);
            documentRepository.save(document);

            log.info("Successfully processed document: {} with {} chunks",
                    document.getOriginalFileName(), savedChunks.size());

        } catch (Exception e) {
            log.error("Failed to process document: {}", document.getOriginalFileName(), e);
            document.setProcessingStatus(ProcessingStatus.FAILED);
            document.setErrorMessage(e.getMessage());
            documentRepository.save(document);
        }
    }

    @Transactional
    public List<DocumentChunk> saveChunks(Document document,
                                          List<TextChunkingService.ChunkInfo> chunkInfos,
                                          User user) {
        List<DocumentChunk> chunks = new ArrayList<>();

        for (TextChunkingService.ChunkInfo info : chunkInfos) {
            DocumentChunk chunk = DocumentChunk.builder()
                    .id(UUID.randomUUID().toString())
                    .document(document)
                    .user(user)
                    .chunkIndex(info.getChunkIndex())
                    .content(info.getContent())
                    .startOffset(info.getStartOffset())
                    .endOffset(info.getEndOffset())
                    .tokenCount(info.getTokenCount())
                    .build();
            chunks.add(chunk);
        }

        return documentChunkRepository.saveAll(chunks);
    }

    private String generateUniqueFileName(String originalName) {
        String extension = "";
        int lastDot = originalName.lastIndexOf('.');
        if (lastDot > 0) {
            extension = originalName.substring(lastDot);
        }
        return UUID.randomUUID() + extension;
    }

    private DocumentType inferDocumentType(String fileName) {
        String lowerName = fileName.toLowerCase();
        if (lowerName.contains("meeting") || lowerName.contains("minutes")) {
            return DocumentType.MEETING_MINUTES;
        } else if (lowerName.contains("proposal")) {
            return DocumentType.PROPOSAL;
        } else if (lowerName.contains("quote") || lowerName.contains("quotation")) {
            return DocumentType.QUOTATION;
        } else if (lowerName.contains("email") || lowerName.contains("mail")) {
            return DocumentType.EMAIL_LOG;
        } else if (lowerName.contains("contract")) {
            return DocumentType.CONTRACT;
        } else if (lowerName.contains("spec") || lowerName.contains("technical")) {
            return DocumentType.TECHNICAL_SPEC;
        }
        return DocumentType.OTHER;
    }
}
