package com.example.sales.repository;

import com.example.sales.model.entity.Deal;
import com.example.sales.model.entity.User;
import com.example.sales.model.enums.DealStage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DealRepository extends JpaRepository<Deal, Long> {

    List<Deal> findByUser(User user);

    Optional<Deal> findByDealIdAndUser(String dealId, User user);

    @Query("SELECT d FROM Deal d WHERE d.user = :user AND d.dealStage NOT IN :closedStages")
    List<Deal> findActiveDeals(@Param("user") User user, @Param("closedStages") List<DealStage> closedStages);

    default List<Deal> findActiveDeals(User user) {
        return findActiveDeals(user, List.of(DealStage.CLOSED_WON, DealStage.CLOSED_LOST));
    }

    boolean existsByDealIdAndUser(String dealId, User user);
}
