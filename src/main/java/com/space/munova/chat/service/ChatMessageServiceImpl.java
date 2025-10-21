package com.space.munova.chat.service;

import com.space.munova.chat.dto.ChatMessageRequestDto;
import com.space.munova.chat.dto.ChatMessageResponseDto;
import com.space.munova.chat.dto.ChatMessageViewDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.MessageRepository;
import com.space.munova.chat.repository.MemberRepository;
import com.space.munova.member.entity.Member;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MemberRepository userRepository;

    // 메시지 DB에 저장
    @Override
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest) {

        // 채팅방 확인
        Chat chatId = chatRepository.findById(chatMessageRequest.getChatId())
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatMessageRequest.getChatId()));
        // 송신자 확인
        Member senderId = userRepository.findById(chatMessageRequest.getSenderId())
                .orElseThrow(() -> ChatException.cannotFindMemberException("senderId=" + chatMessageRequest.getSenderId()));

        // 메시지를 repository에 저장 + 현재 시간
        Message message = messageRepository.save(Message.builder()
                .chatId(chatId)
                .userId(senderId)
                .content(chatMessageRequest.getContent())
                .type(chatMessageRequest.getMessageType())
                .build());

        // 가장 최신 메시지 id, 최근 대화 시간 업데이트
        chatId.modifyLastMessageContent(message.getContent(), message.getCreatedAt());

        return new ChatMessageResponseDto(chatId.getId(), senderId.getId(), message);
    }


    // 채팅방 메시지 List 조회
    @Override
    @Transactional
    public List<ChatMessageViewDto> getMessagesByChatId(Long chatId) {
        return messageRepository.findAllByChatIdWithChat(chatId).stream()
                .map(ChatMessageViewDto::new).toList();

    }

}


