package com.space.munova.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker   // STOMP 기반 websocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    // 클라이언트가 연결할 websocket 엔드 포인트
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {

        registry.addEndpoint("/ws")  // 클라이언트가 실제 연결할 websocket 엔드 포인트를 /ws로 설정
                .setAllowedOriginPatterns("*") // 모든 도메인에서 접속 허용(이후 특정 도메인만 접속 가능하도록 설정할 예정)
                .withSockJS();  // websocket 지원 안되면 fall back 되도록
    }

    // 메모리 기반 Simple message broker를 활성화함
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/mpub");
        registry.enableSimpleBroker("/msub");
    }
}
