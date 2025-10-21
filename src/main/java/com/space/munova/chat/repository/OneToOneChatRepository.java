package com.space.munova.chat.repository;

import com.space.munova.chat.entity.OneToOneChat;
import com.space.munova.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OneToOneChatRepository extends JpaRepository<OneToOneChat, Long> {

    Optional<OneToOneChat> findByBuyerIdAndSellerId(Member buyerId, Member sellerId);
}
