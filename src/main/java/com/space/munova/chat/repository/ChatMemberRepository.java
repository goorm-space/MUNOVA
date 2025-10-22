package com.space.munova.chat.repository;

import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.product.Product;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatMemberRepository extends CrudRepository<ChatMember, Long> {

    Optional<ChatMember> findByMemberIdAndProductId(Member memberId, Product productId);

    @Query("SELECT new com.space.munova.chat.dto.ChatItemDto" +
            "(c.id, c.name, c.lastMessageContent, c.lastMessageTime)" +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.memberId = :memberId " +
            "AND c.status = 'OPENED' " +
            "AND c.type = :chatType " +
            "ORDER BY c.lastMessageTime DESC")
    List<ChatItemDto> findAllChatsByMemberIdAndChatType(
            @Param("memberId") Long memberId,
            @Param("chatType")ChatType chatType);


    // 참여자 여부 확인 (메시지 전송/조회용)
    @Query("SELECT CASE WHEN COUNT(cm) > 0 THEN true ELSE false END " +
            "FROM ChatMember cm " +
            "JOIN cm.chatId c " +
            "WHERE cm.chatId.id = :chatId " +
            "AND cm.memberId.id = :memberId " +
            "AND c.status = 'OPENED'")
    boolean findByChatIdAndParticipantIdWithChat(
            @Param("chatId") Long chatId,
            @Param("memberId") Long memberId);
}
