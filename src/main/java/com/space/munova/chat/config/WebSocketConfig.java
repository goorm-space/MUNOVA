package com.space.munova.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
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

        // 클라이언트가 websocket에 연결하기 위한 엔드포인트를 /ws로 설정
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 모든 도메인에서 접속 허용(이후 특정 도메인만 접속 가능하도록 설정할 예정)
                .withSockJS();  // websocket을 지원하지 않는 브라우저도 SocketJs를 이용해 websocket을 사용하도록 도움
    }

    // 메모리 기반 Simple message broker를 활성화함
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // @MessageMapping 달린 메서드로 라우팅 -> 클라이언트가 서버로 메세지를 보낼 때 사용
        registry.setApplicationDestinationPrefixes("/mpub");

        // /msub/chat 접두사로 시작하는 주제를 구독해 메시지를 받을 수 있음
        registry.enableSimpleBroker("/msub");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new LoggingInterceptor());
    }


}
