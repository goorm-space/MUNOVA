package com.space.munova.recommend.repository;

import com.space.munova.recommend.domain.UserRecommendation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {
    Page<UserRecommendation> findAll(Pageable pageable);
    Page<UserRecommendation> findByMemberId(Long MemberId, Pageable pageable);
    Optional<UserRecommendation> findTopByMemberIdOrderByCreatedAtDesc(Long userId);
    void deleteByMemberId(Long memberId);
}