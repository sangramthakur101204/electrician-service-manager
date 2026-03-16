package com.electrician.servicemanager.repository;

import com.electrician.servicemanager.entity.RateCard;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface RateCardRepository extends JpaRepository<RateCard, Long> {
    List<RateCard> findByCategory(String category);
    List<RateCard> findByIsActiveTrue();
    List<RateCard> findByCategoryAndIsActiveTrue(String category);

    // Owner-specific rate cards
    List<RateCard> findByOwnerIdAndIsActiveTrue(Long ownerId);
    List<RateCard> findByOwnerIdIsNullAndIsActiveTrue();
}