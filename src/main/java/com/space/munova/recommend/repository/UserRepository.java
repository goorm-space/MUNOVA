package com.space.munova.recommend.repository;

import com.space.munova.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserRepository extends JpaRepository<Member, Long> {
    List<Member> findByUsername(String name);
}