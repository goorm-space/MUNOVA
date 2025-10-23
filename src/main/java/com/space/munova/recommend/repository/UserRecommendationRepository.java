package com.space.munova.recommend.repository;

import com.space.munova.recommend.domain.UserRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserRecommendationRepository extends JpaRepository<UserRecommendation, Long> {
    List<UserRecommendation> findByMemberId(Long memberId);

    List<UserRecommendation> findTop10ByMemberIdOrderByScoreDesc(Long memberId);
}