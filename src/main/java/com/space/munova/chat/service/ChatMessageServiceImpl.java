package com.space.munova.chat.service;

import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import com.space.munova.chat.dto.message.ChatMessageResponseDto;
import com.space.munova.chat.dto.message.ChatMessageViewDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatMemberRepository;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.MessageRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MemberRepository userRepository;
    private final ChatMemberRepository chatMemberRepository;

    // 메시지 DB에 저장
    @Override
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest) {

        // 송신자 확인
        Member senderId = userRepository.findById(chatMessageRequest.getSenderId())
                .orElseThrow(() -> ChatException.cannotFindMemberException("senderId=" + chatMessageRequest.getSenderId()));

        // 채팅방 확인
        Chat chatId = chatRepository.findById(chatMessageRequest.getChatId())
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatMessageRequest.getChatId()));

        // 2. 채팅방 상태 확인
        if (chatId.getStatus() != ChatStatus.OPENED) {
            throw ChatException.chatClosedException("chatId=" + chatId.getId());
        }

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


    // 채팅방 메시지 List 조회 (1:1)
    @Override
    @Transactional
    public List<ChatMessageViewDto> getMessagesByChatId(Long chatId, Long memberId) {

        // 참여자 확인
        Member member = userRepository.findById(memberId)
                .orElseThrow(() -> ChatException.cannotFindMemberException("senderId=" + memberId));

        // 1. 채팅방 확인
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatId));

        // 2. 채팅방 상태 확인
        if (chat.getStatus() != ChatStatus.OPENED) {
            throw ChatException.chatClosedException("chatId=" + chatId);
        }

        // 3. 참여자 권한 확인 (1:1 채팅)
        if (!chatMemberRepository.existsBy(chatId, memberId, ChatStatus.OPENED)) {
            throw ChatException.unauthorizedParticipantException("userId=" + memberId);
        }

        return messageRepository.findAllByChatIdWithChat(chatId).stream()
                .map(ChatMessageViewDto::new).toList();

    }

}


