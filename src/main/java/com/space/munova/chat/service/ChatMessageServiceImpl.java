package com.space.munova.chat.service;

import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import com.space.munova.chat.dto.message.ChatMessageResponseDto;
import com.space.munova.chat.dto.message.ChatMessageViewDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.MessageRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MemberRepository memberRepository;

    // 메시지 DB에 저장
    @Override
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest, Long chatId, Long memberId) {

        Member sender = getMemberOrThrow(memberId);
        Chat chat = getOpenedChatForMember(chatId, memberId);
        Message message = saveMessage(chatMessageRequest, chat, sender);
        chat.modifyLastMessageContent(message.getContent(), message.getCreatedAt());

        return ChatMessageResponseDto.of(chat, sender, message);
    }


    // 채팅방 메시지 List 조회 (1:1)
    @Override
    @Transactional
    public List<ChatMessageViewDto> getMessagesByChatId(Long chatId, Long memberId) {

        // 채팅방 확인, OPENED 확인
        Chat chat = getOpenedChatForMember(chatId, memberId);
        return messageRepository.findAllByChatId(chatId);
    }


    // private Helper
    private Member getMemberOrThrow(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> MemberException.notFoundException("memberId : " + memberId));
    }

    private Chat getOpenedChatForMember(Long chatId, Long memberId) {
        return chatRepository.findByChatIdAndChatStatus(chatId, memberId, ChatStatus.OPENED)
                .orElseThrow(() -> ChatException.unauthorizedParticipantException("chatId:" + chatId));
    }

    private Message saveMessage(ChatMessageRequestDto request, Chat chat, Member sender) {
        return messageRepository.save(
                Message.createMessage(request.content(), request.messageType(), chat, sender)
        );
    }
}


