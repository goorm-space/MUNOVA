package com.space.munova.chat.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.chat.dto.ChatEvent;
import com.space.munova.chat.dto.message.ChatMessageRequestDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHandler extends TextWebSocketHandler {

    private final SessionManager sessionManager;
    private final ObjectMapper objectMapper;
    private final ChatMessageBroker chatMessageBroker;

    /**
     * 메시지 수신 처리 스레드풀 (JSON 파싱 + 비즈니스)
     * CPU 코어 2배 정도가 권장
     */
    private final ExecutorService inboundPool =
//            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            Executors.newFixedThreadPool(16);


    // 세션 CONNECT
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessionManager.addSession(session);
        session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\"}"));
    }

    // 메시지 처리
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        String payload = message.getPayload();

        // heartbeat
        if (payload.trim().isEmpty()) {
            sessionManager.touchHeartbeat(session.getId());
            return;
        }

        sessionManager.touchHeartbeat(session.getId());
        inboundPool.submit(() -> {
            try {
                // JSON parsing
                ChatMessageRequestDto dto =
                        objectMapper.readValue(payload, ChatMessageRequestDto.class);
                sessionManager.touchHeartbeat(session.getId());

                switch (dto.messageType()) {
                    case SEND -> handleMessage(session, dto);
                    case SUBSCRIBE -> handleSubscribe(session, dto);
                    case UNSUBSCRIBE -> handleUnsubscribe(session, dto);
                    default -> log.warn("Invalid message type: {}", dto.messageType());
                }

            } catch (Exception e) {
                log.error("Failed to process inbound message: {}", e.getMessage());
            }
        });
    }

    // 메시지 발행
    private void handleMessage(WebSocketSession session, ChatMessageRequestDto dto) {
        chatMessageBroker.publish(new ChatEvent(dto.chatId(), buildOutboundJson(dto)));
        log.info("SEND: chatId={}, senderId={}", dto.chatId(), dto.senderId());
    }

    // JSON -> Java 변환
    private String buildOutboundJson(ChatMessageRequestDto dto) {
        try {
            return objectMapper.writeValueAsString(Map.of(
                    "chatId", dto.chatId(),
                    "senderId", dto.senderId(),
                    "content", dto.content(),
                    "clientTs", dto.clientTs(),
                    "serverTs", System.currentTimeMillis(),
                    "messageType", dto.messageType().name()
            ));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // 구독
    private void handleSubscribe(WebSocketSession session, ChatMessageRequestDto dto) {
        Long chatId = dto.chatId();
        sessionManager.subscribe(session.getId(), chatId);
        log.info("SUBSCRIBE: session={} -> chatId={}", session.getId(), chatId);
    }

    // 구독 해제
    private void handleUnsubscribe(WebSocketSession session, ChatMessageRequestDto dto) {
        Long chatId = dto.chatId();
        sessionManager.unSubscribe(session.getId(), chatId);

        log.info("UNSUBSCRIBE: session={} -> chatId={}", session.getId(), chatId);
    }


    // DISCONNECT
    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessionManager.removeSession(session.getId());
        log.info("WS DISCONNECTED: {}", session.getId());
    }
}
