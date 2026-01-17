package com.example.sales.controller;

import com.example.sales.model.dto.DocumentResponse;
import com.example.sales.model.entity.Document;
import com.example.sales.model.entity.User;
import com.example.sales.repository.DocumentRepository;
import com.example.sales.service.DocumentProcessingService;
import com.example.sales.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentProcessingService documentProcessingService;
    private final DocumentRepository documentRepository;
    private final FileService fileService;

    @PostMapping("/upload")
    public ResponseEntity<DocumentResponse> uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dealId", required = false) String dealId,
            @AuthenticationPrincipal User user
    ) {
        fileService.validateFile(file);

        if (!fileService.isPdfFile(file)) {
            throw new IllegalArgumentException("Only PDF files are supported for document upload");
        }

        Document document = documentProcessingService.createDocument(file, user, dealId);
        documentProcessingService.processDocumentAsync(document.getId(), file, user);

        return ResponseEntity.accepted().body(toResponse(document));
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> listDocuments(
            @AuthenticationPrincipal User user
    ) {
        List<Document> documents = documentRepository.findByUser(user);
        List<DocumentResponse> responses = documents.stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal User user
    ) {
        Document document = documentRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        return ResponseEntity.ok(toResponse(document));
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.builder()
                .id(document.getId())
                .fileName(document.getOriginalFileName())
                .documentType(document.getDocumentType() != null ?
                        document.getDocumentType().name() : null)
                .processingStatus(document.getProcessingStatus().name())
                .totalPages(document.getTotalPages())
                .totalChunks(document.getTotalChunks())
                .fileSize(document.getFileSize())
                .createdAt(document.getCreatedAt())
                .dealId(document.getDeal() != null ? document.getDeal().getDealId() : null)
                .build();
    }
}
