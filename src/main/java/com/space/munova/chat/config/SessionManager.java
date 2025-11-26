package com.space.munova.chat.config;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Getter
@Component
public class SessionManager {

    private final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Set<Long>> sessionSubscriptions = new ConcurrentHashMap<>();

    // ★ 변경: COW 기반
    private final ConcurrentHashMap<Long, CopyOnWriteArraySet<WebSocketSession>> chatRoomSessions = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Long> heartbeat = new ConcurrentHashMap<>();

    // CONNECT
    public void addSession(WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        heartbeat.put(sessionId, System.currentTimeMillis());
        sessionSubscriptions.put(sessionId, ConcurrentHashMap.newKeySet());
    }

    // SUBSCRIBE
    public void subscribe(String sessionId, Long chatId) {
        WebSocketSession session = sessions.get(sessionId);
        if (session == null) return;

        sessionSubscriptions.get(sessionId).add(chatId);

        chatRoomSessions
                .computeIfAbsent(chatId, id -> new CopyOnWriteArraySet<>())
                .add(session);
    }

    // UNSUBSCRIBE
    public void unSubscribe(String sessionId, Long chatId) {
        Set<Long> subs = sessionSubscriptions.get(sessionId);
        if (subs != null) subs.remove(chatId);

        CopyOnWriteArraySet<WebSocketSession> wsSet = chatRoomSessions.get(chatId);
        if (wsSet != null) {
            wsSet.removeIf(ws -> ws.getId().equals(sessionId));
        }
    }

    // DISCONNECT
    public void removeSession(String sessionId) {
        heartbeat.remove(sessionId);

        Set<Long> rooms = sessionSubscriptions.getOrDefault(sessionId, Set.of());
        for (Long chatId : rooms) {
            CopyOnWriteArraySet<WebSocketSession> wsSet = chatRoomSessions.get(chatId);
            if (wsSet != null) {
                wsSet.removeIf(ws -> ws.getId().equals(sessionId));
            }
        }

        WebSocketSession s = sessions.remove(sessionId);
        sessionSubscriptions.remove(sessionId);

        try {
            if (s != null) s.close();
        } catch (Exception ignored) {
        }
    }

    // Heartbeat update
    public void touchHeartbeat(String sessionId) {
        heartbeat.put(sessionId, System.currentTimeMillis());
    }

    // Broadcast 시 호출
    public Set<WebSocketSession> getSessionsForChat(Long chatId) {
        return chatRoomSessions.getOrDefault(chatId, new CopyOnWriteArraySet<>());
    }
}