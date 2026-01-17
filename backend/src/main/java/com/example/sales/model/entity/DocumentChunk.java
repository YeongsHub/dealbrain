package com.example.sales.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_chunks", indexes = {
    @Index(name = "idx_chunk_document_id", columnList = "document_id"),
    @Index(name = "idx_chunk_user_id", columnList = "user_id")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentChunk {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "page_number")
    private Integer pageNumber;

    @Column(name = "start_offset")
    private Integer startOffset;

    @Column(name = "end_offset")
    private Integer endOffset;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
