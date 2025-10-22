package com.space.munova.chat.repository;

import com.space.munova.chat.dto.OneToOneChatItemDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.OneToOneChat;
import com.space.munova.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OneToOneChatRepository extends JpaRepository<OneToOneChat, Long> {

    Optional<OneToOneChat> findByBuyerIdAndSellerId(Member buyerId, Member sellerId);

    // 참여자 여부 확인 (메시지 전송/조회용)
    @Query("SELECT CASE WHEN COUNT(o) > 0 THEN true ELSE false END " +
            "FROM OneToOneChat o " +
            "JOIN o.chatId c " +
            "WHERE o.chatId.id = :chatId " +
            "AND ((o.buyerId.id = :memberId AND c.status = 'OPENED') " +
            "OR (o.sellerId.id = :memberId AND c.status = 'OPENED'))")
    boolean findByChatIdAndParticipantIdWithChat(@Param("chatId") Long chatId, @Param("memberId") Long memberId);

    List<OneToOneChat> findBySellerId(Member seller);

    @Query("SELECT o " +
            "FROM OneToOneChat o " +
            "JOIN FETCH o.chatId c " +
            "WHERE o.sellerId.id = :sellerId " +
            "ORDER BY c.lastMessageTime DESC")
    List<OneToOneChat> findAllWithChatBySellerId(@Param("sellerId") Long sellerId);


    @Query("SELECT new com.space.munova.chat.dto.OneToOneChatItemDto" +
            "(c.id, c.name, c.lastMessageContent, c.lastMessageTime) " +
            "FROM OneToOneChat o " +
            "JOIN o.chatId c " +
            "WHERE o.sellerId.id = :sellerId " +
            "AND c.status = 'OPENED'")
    List<OneToOneChatItemDto> findAllChatDtosBySellerId(@Param("sellerId") Long sellerId);
}
