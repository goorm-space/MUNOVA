package com.space.munova.chat.service;

import com.space.munova.chat.dto.OneToOneChatItemDto;
import com.space.munova.chat.dto.OneToOneChatRequestDto;
import com.space.munova.chat.dto.OneToOneChatResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.OneToOneChat;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatRepository;
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
                                            .userId(seller)
                                            .build());
        // 채팅방 참가자 등록
        oneToOneChatRepository.save(new OneToOneChat(chat, buyer, seller));

        return OneToOneChatResponseDto.to(chat, buyer, seller);
    }

    @Override
    public List<OneToOneChatItemDto> getOneToOneChatRooms(Long userId) {

        // 사용자 조회
        Member buyer = userRepository.findById(userId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("buyerId=" + userId));

        // 사용자를 통해 Chat 리스트 조회
        return chatRepository.findAllByUserIdOrderByLastMessageTimeDesc(buyer.getId())
                .stream().map(OneToOneChatItemDto::new).toList();
    }


    private String generateChatRoomName(Product product, Member otherUser) {
        return "[" + product.getName() + "] 문의 - " + otherUser.getUsername() + "님";
    }
}
