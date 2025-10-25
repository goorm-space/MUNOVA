package com.space.munova.recommend.repository;

import com.space.munova.recommend.domain.UserRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {

    Optional<UserRecommendation> findTopByMemberIdOrderByCreatedAtDesc(Long userId);
}