package com.space.munova.chat.service;

import com.space.munova.chat.dto.ChatMessageRequestDto;
import com.space.munova.chat.dto.ChatMessageResponseDto;
import com.space.munova.chat.entity.Chat;
import com.space.munova.chat.entity.Message;
import com.space.munova.chat.entity.User;
import com.space.munova.chat.repository.ChatRepository;
import com.space.munova.chat.repository.MessageRepository;
import com.space.munova.chat.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {

    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public ChatMessageResponseDto createChatMessage(ChatMessageRequestDto chatMessageRequest) {

        Optional<Chat> chatId = chatRepository.findById(chatMessageRequest.getChatId());
        Optional<User> senderId = userRepository.findById(chatMessageRequest.getSenderId());

        // 메시지를 repository에 저장 + 현재 시간
        messageRepository.save(Message.builder()
                        .chatId(chatId.get())
                        .userId(senderId.get())
                        .content(chatMessageRequest.getContent())
                        .type(chatMessageRequest.getMessageType())
                        .build());

        return null;
    }
}
