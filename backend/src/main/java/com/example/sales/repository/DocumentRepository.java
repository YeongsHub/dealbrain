package com.example.sales.repository;

import com.example.sales.model.entity.Document;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByUser(User user);

    List<Document> findByUserAndProcessingStatus(User user, ProcessingStatus status);

    Optional<Document> findByIdAndUser(Long id, User user);

    @Query("SELECT d FROM Document d WHERE d.user = :user AND d.deal.dealId = :dealId")
    List<Document> findByUserAndDealId(@Param("user") User user, @Param("dealId") String dealId);

    boolean existsByFileNameAndUser(String fileName, User user);
}
