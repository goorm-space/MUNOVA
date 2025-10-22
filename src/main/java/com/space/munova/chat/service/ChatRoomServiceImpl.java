package com.space.munova.chat.service;

import com.space.munova.chat.dto.*;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.GroupChat;
import com.space.munova.chat.entity.OneToOneChat;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.enums.ChatUserType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.GroupChatRepository;
import com.space.munova.chat.repository.OneToOneChatRepository;
import com.space.munova.chat.repository.MemberRepository;
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
    private final OneToOneChatRepository oneToOneChatRepository;
    private final GroupChatRepository groupChatRepository;
    private final JpaProductRepository productRepository;

    // 1:1 채팅방 생성
    @Override
    @Transactional
    public OneToOneChatResponseDto createOneToOneChatRoom(OneToOneChatRequestDto requestChatRoomDto) {

        // 판매자, 구매자, 상품 조회
        Member buyer = userRepository.findById(requestChatRoomDto.getBuyerId())
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + requestChatRoomDto.getBuyerId()));
        Member seller = userRepository.findById(requestChatRoomDto.getSellerId())
                .orElseThrow(() -> ChatException.cannotFindMemberException("sellerId=" + requestChatRoomDto.getSellerId()));
        Product product = productRepository.findById(requestChatRoomDto.getProductId())
                .orElseThrow(() -> ChatException.cannotFindProductException("productId :" +  requestChatRoomDto.getProductId()));

        // 채팅장 이미 있는지 확인
        Optional<OneToOneChat> existingChat = oneToOneChatRepository.findByBuyerIdAndSellerId(buyer, seller);

        // 있으면 기존 채팅방 반환
        if(existingChat.isPresent()) {
            return OneToOneChatResponseDto.to(existingChat.get().getChatId(), buyer, seller);
        }

        // 1:1 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                        .name(generateChatRoomName(product, buyer))
                        .type(ChatType.ONE_ON_ONE)
                        .status(ChatStatus.OPENED)
                        .max_participant(2)
                        .cur_participant(2)
                        .userId(seller)
                        .build());
        // 채팅방 참가자 등록
        oneToOneChatRepository.save(new OneToOneChat(chat, buyer, seller));

        return OneToOneChatResponseDto.to(chat, buyer, seller);
    }

    // 1:1 채팅방 목록 조회(구매자)
    @Override
    @Transactional
    public List<OneToOneChatItemDto> getOneToOneChatRoomsbyBuyer(Long buyerId) {

        // 사용자 조회
        Member buyer = userRepository.findById(buyerId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + buyerId));

        // 사용자를 통해 Chat 리스트 조회
        return chatRepository.findAllByUserIdOrderByLastMessageTimeDesc(buyer.getId())
                .stream().map(OneToOneChatItemDto::new).toList();
    }

    // 1:1 채팅방 목록 조회(판매자)
    @Transactional
    @Override
    public List<OneToOneChatItemDto> getOneToOneChatRoomsbySeller(Long sellerId) {
        // 사용자 조회
        Member buyer = userRepository.findById(sellerId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("sellerId=" + sellerId));

//        return oneToOneChatRepository.findAllWithChatBySellerId(sellerId)
//                .stream().map(m -> new OneToOneChatItemDto(m.getChatId())).toList();
        return oneToOneChatRepository.findAllChatDtosBySellerId(sellerId);

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

        // Group_Chat 테이블 저장
        GroupChat save = groupChatRepository.save(new GroupChat(chat, member, ChatUserType.OWNER));

        return GroupChatResponseDto.to(chat, requestDto.getMemberId());
    }


    private String generateChatRoomName(Product product, Member otherUser) {
        return "[" + product.getName() + "] 문의 - " + otherUser.getUsername() + "님";
    }
}
