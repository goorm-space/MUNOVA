package com.space.munova.recommend.repository;

import com.space.munova.recommend.domain.UserRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {
    List<UserRecommendation> findByMemberId(Long MemberId);

    Optional<UserRecommendation> findTopByMemberIdOrderByCreatedAtDesc(Long userId);
}