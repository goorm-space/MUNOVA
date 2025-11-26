package com.space.munova.chat.config;


import com.space.munova.chat.dto.ChatEvent;
import org.springframework.stereotype.Component;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Component
public class ChatMessageBroker {

    // 메시지가 들어올 때까지 스레드를 Block 상태로 대기
    private final BlockingQueue<ChatEvent> queue = new LinkedBlockingQueue<>();
    private final BlockingQueue<ChatEvent> groupChatQueue = new LinkedBlockingQueue<>();

    public void publish(ChatEvent event) {
        try {
            queue.put(event);
        } catch (InterruptedException ignored) {
        }
    }

    public ChatEvent take() throws InterruptedException {
        return queue.take(); // blocking until data available
    }
}
