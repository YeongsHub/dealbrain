package com.example.sales.repository;

import com.example.sales.model.entity.Document;
import com.example.sales.model.entity.DocumentChunk;
import com.example.sales.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, String> {

    List<DocumentChunk> findByDocument(Document document);

    List<DocumentChunk> findByUser(User user);

    void deleteByDocument(Document document);

    @Query("SELECT c.id FROM DocumentChunk c WHERE c.user = :user")
    List<String> findChunkIdsByUser(@Param("user") User user);

    @Query("SELECT c.id FROM DocumentChunk c WHERE c.document = :document")
    List<String> findChunkIdsByDocument(@Param("document") Document document);

    int countByDocument(Document document);
}
