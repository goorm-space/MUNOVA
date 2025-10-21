package com.space.munova.chat.service;

import com.space.munova.chat.dto.OneToOneChatRoomRequestDto;
import com.space.munova.chat.dto.OneToOneChatRoomResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.OneToOneChat;
import com.space.munova.chat.entity.User;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.OneToOneChatRepository;
import com.space.munova.chat.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatRoomServiceImpl implements ChatRoomService {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;
    private final OneToOneChatRepository oneToOneChatRepository;

    // 1:1 채팅방 생성
    @Override
    @Transactional
    public OneToOneChatRoomResponseDto createOneToOneChatRoom(OneToOneChatRoomRequestDto requestChatRoomDto) {

        // 판매자, 구매자 조회
        User buyer = userRepository.findById(requestChatRoomDto.getBuyerId())
                .orElseThrow(() -> new RuntimeException("구매자 없음"));
        User seller = userRepository.findById(requestChatRoomDto.getSellerId())
                .orElseThrow(() -> new RuntimeException("판매자 없음"));

        // 채팅장 이미 있는지 확인
        Optional<OneToOneChat> existingChat = oneToOneChatRepository.findByBuyerIdAndSellerId(buyer, seller);

        // 있으면 기존 채팅방 반환
        if(existingChat.isPresent()) {
            return OneToOneChatRoomResponseDto.to(existingChat.get().getChatId(), buyer, seller);
        }

        // 채팅방 생성
        Chat chat = chatRepository.save(Chat.builder()
                                            .name(requestChatRoomDto.getName())
                                            .type(requestChatRoomDto.getType())
                                            .status(ChatStatus.OPENED)
                                            .build());
        // 채팅방 참가자 등록
        oneToOneChatRepository.save(new OneToOneChat(chat, buyer, seller));

        return OneToOneChatRoomResponseDto.to(chat, buyer, seller);
    }
}
