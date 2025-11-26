package com.space.munova.chat.config;

import com.space.munova.chat.dto.ChatEvent;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ExecutorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageBrokerWorker {

    private final ChatMessageBroker chatMessageBroker;
    private final SessionManager sessionManager;
    private final ExecutorService messageBrokerExecutor;

    // I/O 전송 전용 스레드풀
    private final ExecutorService outboundExecutor =
            java.util.concurrent.Executors.newFixedThreadPool(8);

    @PostConstruct
    public void startWorker() {
        // CPU 수 만큼 워커 쓰레드 띄우기
        int workers = Runtime.getRuntime().availableProcessors();

        for (int i = 0; i < workers; i++) {
            messageBrokerExecutor.submit(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        ChatEvent event = chatMessageBroker.take(); // blocking -> 이벤트 방식 -> 구현 변경 예정
                        broadcast(event);
                    } catch (Exception e) {
                        log.error("Broker worker error: {}", e.getMessage());
                    }
                }
            });
        }
    }

    private void broadcast(ChatEvent event) {
        Set<WebSocketSession> sessions = sessionManager.getChatRoomSessions().get(event.chatId());

        if (sessions == null || sessions.isEmpty()) return;

        for (WebSocketSession ws : sessions) {
            if (ws.isOpen()) {
                outboundExecutor.submit(() -> {   // ★ I/O 스레드 분리
                    try {
                        ws.sendMessage(new TextMessage(event.jsonPayload()));
                    } catch (Exception e) {
                        log.error("Send failed: {}", e.getMessage());
                        sessionManager.removeSession(ws.getId());
                    }
                });
            }
        }
    }
}
