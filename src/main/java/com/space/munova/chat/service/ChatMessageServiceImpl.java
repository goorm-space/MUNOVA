package com.space.munova.chat.service;

import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import com.space.munova.chat.dto.message.ChatMessageResponseDto;
import com.space.munova.chat.dto.message.ChatMessageViewDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.enums.ChatStatus;
import com.space.munova.chat.enums.ChatType;
import com.space.munova.chat.exception.ChatException;
import com.space.munova.chat.repository.ChatMemberRepository;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.MessageRepository;
import com.space.munova.member.entity.Member;
import com.space.munova.member.exception.MemberException;
import com.space.munova.member.repository.MemberRepository;
import com.space.munova.security.jwt.JwtHelper;
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
    private final ChatMemberRepository chatMemberRepository;

    // 메시지 DB에 저장
    @Override
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest, Long chatId) {

        Member member = memberRepository.findById(chatMessageRequest.senderId())
                .orElseThrow(() -> MemberException.notFoundException("memberId : " + chatMessageRequest.senderId()));

        // 채팅방 확인
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> ChatException.cannotFindChatException("chatId=" + chatId));

        // 2. 채팅방 상태 확인
        if (chat.getStatus() != ChatStatus.OPENED) {
            throw ChatException.chatClosedException("chatId=" + chat.getId());
        }

        // 메시지를 repository에 저장 + 현재 시간
        Message message = messageRepository.save(Message.createMessage(chatMessageRequest.content(), chatMessageRequest.messageType(), chat, member));

        // 가장 최신 메시지 id, 최근 대화 시간 업데이트
        chat.modifyLastMessageContent(message.getContent(), message.getCreatedAt());

        return ChatMessageResponseDto.of(chat.getId(), member.getId(), member.getUsername(), message.getContent(), message.getCreatedAt(), message.getType());
    }


    // 채팅방 메시지 List 조회 (1:1)
    @Override
    @Transactional
    public List<ChatMessageViewDto> getMessagesByChatId(Long chatId) {

        Long memberId = JwtHelper.getMemberId();

        // 채팅방 확인, OPENED 확인
        Chat chat = chatRepository.findChatByIdAndType(chatId, ChatType.ONE_ON_ONE)
                .orElseThrow(() -> ChatException.invalidChatRoomException("chatId=" + chatId));

        // 참여자 권한 확인
        if (!chatMemberRepository.existsChatMemberAndMemberIdBy(chatId, memberId)) {
            throw ChatException.unauthorizedParticipantException("userId=" + memberId);
        }

        return messageRepository.findAllByChatId(chatId);
    }

}


