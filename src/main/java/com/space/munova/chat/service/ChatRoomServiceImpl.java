package com.space.munova.chat.service;

import com.space.munova.chat.dto.group.GroupChatRequestDto;
import com.space.munova.chat.dto.group.GroupChatResponseDto;
import com.space.munova.chat.dto.ChatItemDto;
import com.space.munova.chat.dto.onetoone.OneToOneChatResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.ChatMember;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.*;
import com.space.munova.member.dto.MemberRole;
import com.space.munova.member.entity.Member;
import com.space.munova.product.domain.product.Jpa.JpaProductRepository;
import com.space.munova.product.domain.product.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final MemberRepository userRepository;
    private final ChatRepository chatRepository;
    private final JpaProductRepository productRepository;
    private final ChatMemberRepository chatMemberRepository;

    // 1:1 채팅방 생성
    @Override
    @Transactional
    public OneToOneChatResponseDto createOneToOneChatRoom(Long memberId, Long productId) {

        // 구매자 조회
        Member buyer = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        // 상품 조회
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> ChatException.cannotFindProductException("productId :" + productId));

//        // 판매자 조회
//        Member seller = userRepository.findById(product.)
//                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        // 채팅방 이미 있는지 확인
        Optional<ChatMember> existingChat = chatMemberRepository.findByMemberIdAndProductId(buyer, product);

        // 있으면 기존 채팅방 반환
        if(existingChat.isPresent()) {
            return OneToOneChatResponseDto.to(existingChat.get().getChatId(), buyer, buyer);
        }

        // 1:1 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                        .name(generateChatRoomName(product, buyer))
                        .type(ChatType.ONE_ON_ONE)
                        .status(ChatStatus.OPENED)
                        .max_participant(2)
                        .cur_participant(2)
                        .userId(buyer)
                        .build());
        // 채팅방 참가자(판매자) 등록
        chatMemberRepository.save(new ChatMember(chat, buyer, ChatUserType.MEMBER));

        return OneToOneChatResponseDto.to(chat, buyer, buyer);
    }

    // 1:1 채팅방 목록 조회(구매자)
    @Override
    @Transactional
    public List<ChatItemDto> getOneToOneChatRoomsByBuyer(Long buyerId) {

        // 사용자 조회
        Member buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + buyerId));

        // 사용자 아이디로 Chat 리스트 조회
        return chatRepository.findAllByUserIdOrderByLastMessageTimeDesc(buyer.getId());
    }

    // 1:1 채팅방 목록 조회(판매자)
    @Transactional
    @Override
    public List<ChatItemDto> getOneToOneChatRoomsBySeller(Long sellerId) {
        // 사용자 조회
        Member seller = userRepository.findById(sellerId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("sellerId=" + sellerId));

        // 판매자 아이디로 Chat 리스트 조회
        return chatMemberRepository.findAllChatsByMemberIdAndChatType(sellerId, ChatType.ONE_ON_ONE);
    }

    // group 채팅방 생성
    @Override
    @Transactional
    public GroupChatResponseDto createGroupChatRoom(GroupChatRequestDto requestDto) {

        // 채팅방 생성자 조회
        Member member = userRepository.findById(requestDto.getMemberId())
                .orElseThrow(() -> ChatException.cannotFindMemberException("sellerId=" + requestDto.getMemberId()));

        // Group 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                        .name(requestDto.getName())
                        .type(ChatType.GROUP)
                        .status(ChatStatus.OPENED)
                        .userId(member)
                        .cur_participant(1)
                        .max_participant(requestDto.getMaxParticipants())
                        .build());

        return GroupChatResponseDto.to(chat, requestDto.getMemberId());
    }

    // 그룹 채팅 목록 조회
    @Override
    @Transactional
    public List<ChatItemDto> getGroupChatRooms(Long memberId) {

        // 사용자 조회
        Member buyer = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        return chatMemberRepository.findAllChatsByMemberIdAndChatType(memberId, ChatType.GROUP);
    }


    @Override
    public OneToOneChatResponseDto setChatRoomClosed(Long memberId, Long chatId) {
        // 해당 멤버가 실제로 존재하고 판매자인지,
        Member seller = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + memberId));

        if(seller.getRole() != MemberRole.SELLER) {
            throw ChatException.unauthorizedAccessException("sellerId=" + seller.getId());
        }

        // 채팅방 존재하는지
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatId));

        // 해당 상품이 이 판매자의 판매 물건이 맞는지

        // 채팅방 상태 변경

        // 리턴


        return null;
    }


    private String generateChatRoomName(Product product, Member otherUser) {
        return "[" + product.getName() + "] 문의 - " + otherUser.getUsername() + "님";
    }
}
