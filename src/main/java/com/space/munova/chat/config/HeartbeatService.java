package com.space.munova.chat.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class HeartbeatService {

    private final SessionManager sessionManager;
    private final ScheduledExecutorService heartbeatSenderExecutor;
    private final ScheduledExecutorService heartbeatCheckerExecutor;

    private static final long HEARTBEAT_INTERVAL = 10_000L;
    private static final long HEARTBEAT_TIMEOUT = 30_000L;


    @PostConstruct
    public void init(){
        heartbeatSenderExecutor.scheduleAtFixedRate(() -> {
            try{
                checkHeartbeat();
            } catch (Exception e){
                log.error("Heartbeat check failed: {}", e.getMessage());
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);


        heartbeatCheckerExecutor.scheduleAtFixedRate(() -> {
            try{
                sendHeartbeat();
            } catch (Exception e){
                log.error("Heartbeat send failed");
            }
        }, HEARTBEAT_INTERVAL, HEARTBEAT_INTERVAL, TimeUnit.MILLISECONDS);

    }

    private void sendHeartbeat(){
        sessionManager.getSessions().forEach((sessionId, session) -> {
            if(session.isOpen()){
                try {
                    session.sendMessage(new TextMessage("\n"));
                } catch (Exception e) {
                    sessionManager.removeSession(sessionId);
                    log.error("Heartbeat send failed: {}", e.getMessage());
                }
            }
        });
    }

    private void checkHeartbeat(){
        long now = System.currentTimeMillis();

        sessionManager.getHeartbeat().forEach((sessionId, lastBeat) -> {
            if(now - lastBeat > HEARTBEAT_TIMEOUT) {
                sessionManager.removeSession(sessionId);
                log.warn("Heartbeat timeout, closing session {}", sessionId);
            }
        });
    }

}
