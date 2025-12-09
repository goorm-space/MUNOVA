package com.space.munova.log.infra;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.log.exception.KafkaProducerException;
import com.space.munova.log.infra.dto.proto.UserActionLogProto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionKafkaProducer {

    private static final String TOPIC_USER_ACTION_LOG = "user_action_log";
    private static final String TOPIC_DLQ = "dlq_user_action";

    private final KafkaTemplate<String, UserActionLogProto.UserActionLog> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;

    private Counter logSendSuccessCounter;
    private Counter logSendFailureCounter;
    private Counter logSendDlqCounter;

    @PostConstruct
    public void initMetrics() {
        logSendSuccessCounter = Counter.builder("kafka.producer.send.success")
                .description("Kafka 로그 전송 성공 횟수")
                .tag("component", "kafka_producer")
                .tag("topic", TOPIC_USER_ACTION_LOG)
                .register(meterRegistry);

        logSendFailureCounter = Counter.builder("kafka.producer.send.failure")
                .description("Kafka 로그 전송 실패 횟수")
                .tag("component", "kafka_producer")
                .tag("topic", TOPIC_USER_ACTION_LOG)
                .register(meterRegistry);

        logSendDlqCounter = Counter.builder("kafka.producer.send.dlq")
                .description("Kafka DLQ 전송 횟수")
                .tag("component", "kafka_producer")
                .tag("topic", TOPIC_DLQ)
                .register(meterRegistry);
    }

    public void sendLog(Map<String, Object> logData) {
        try {
            UserActionLogProto.UserActionLog protoMessage = convertToProto(logData); /// Protobuf 메시지로 변환
            String key = generateKey(protoMessage); /// 파티션 키 생성

            // 주기적으로 호출 확인 로그 (10초마다)
            if (System.currentTimeMillis() % 10000 < 100) {
                log.info("📤 Kafka Producer 호출 (Protobuf): eventType={}, memberId={}", 
                        protoMessage.getEventType(), protoMessage.getMemberId());
            }

            kafkaTemplate.send(TOPIC_USER_ACTION_LOG, key, protoMessage).whenComplete((result, exception) -> {
                if (exception != null) {
                    logSendFailureCounter.increment();
                    handleFailure(protoMessage, exception);
                } else {
                    logSendSuccessCounter.increment();
                    // 부하 테스트 중에는 주기적으로 로그 출력 (10초마다)
                    if (System.currentTimeMillis() % 10000 < 100) {
                        log.info("✅ Kafka 로그 전송 성공 (Protobuf): topic={}, partition={}, offset={}, eventType={}",
                                result.getRecordMetadata().topic(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset(),
                                protoMessage.getEventType());
                    }
                }
            });

        } catch (Exception e) {
            logSendFailureCounter.increment();
            log.warn("Kafka 로그 전송 실패: {}", e.getMessage(), e);
        }
    }

    /// Map<String, Object>를 Protobuf 메시지로 변환
    /// Map<String, Object> data는 JSON 문자열로 직렬화하여 저장
    private UserActionLogProto.UserActionLog convertToProto(Map<String, Object> logData) {
        Instant now = Instant.now();
        long producerTimeMs = System.currentTimeMillis();
        
        // memberId 처리 (nullable은 -1로 처리)
        Long memberId = getLongValue(logData.get("member_id"));
        long memberIdValue = (memberId != null) ? memberId : -1L;
        
        // Map<String, Object> data를 JSON 문자열로 변환
        String dataJson = "";
        try {
            Object dataObj = logData.get("data");
            if (dataObj != null) {
                dataJson = objectMapper.writeValueAsString(dataObj);
            }
        } catch (Exception e) {
            log.warn("data JSON 직렬화 실패: {}", e.getMessage());
            dataJson = "{}";
        }
        
        return UserActionLogProto.UserActionLog.newBuilder()
                .setEventType((String) logData.getOrDefault("event_type", ""))
                .setService((String) logData.getOrDefault("service", ""))
                .setMemberId(memberIdValue)
                .setDataJson(dataJson)
                .setEventTimestamp(now.toEpochMilli())
                .setProducerTime(producerTimeMs)
                .setVersion(1)
                .build();
    }

    /// memberId 기준으로 key 생성
    private String generateKey(UserActionLogProto.UserActionLog protoMessage) {
        long memberId = protoMessage.getMemberId();
        if (memberId > 0) {  // -1은 null을 의미
            return String.valueOf(memberId);
        }
        return String.valueOf(System.currentTimeMillis());
    }

    /// DLQ
    private void handleFailure(UserActionLogProto.UserActionLog protoMessage, Throwable exception) {
        log.warn("Kafka 전송 실패 -> DLQ 시도: eventType={}, memberId={}", 
                protoMessage.getEventType(), protoMessage.getMemberId());
        int maxRetry = 3;
        int attempt = 0;
        boolean success = false;
        while (attempt < maxRetry) {
            attempt++;
            try {
                kafkaTemplate.send(TOPIC_DLQ, generateKey(protoMessage), protoMessage).get();
                logSendDlqCounter.increment();
                log.warn("DLQ 전송 성공 (attempt {}): {}", attempt, protoMessage.getMemberId());
                success = true;
                break;
            } catch (Exception e) {
                log.error("DLQ 전송 실패 attempt {}: {}", attempt, e.getMessage());
                if (attempt == maxRetry) throw KafkaProducerException.dlqFailure(e);
            }
        }
        if (!success) {
            writeFallback(protoMessage);
        }
    }

    private void writeFallback(UserActionLogProto.UserActionLog protoMessage) {
        String content = String.format("eventType=%s, memberId=%d, eventTimestamp=%d%n",
                protoMessage.getEventType(),
                protoMessage.getMemberId(),
                protoMessage.getEventTimestamp());
        try {
            Files.writeString(
                    Path.of("/log/kafka_fallback.log"),
                    content,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
            log.error("DLQ 실패 -> fallback 저장 완료: {}", protoMessage.getEventType());
        } catch (IOException e) {
            throw KafkaProducerException.fallbackFailure(e);
        }
    }

    /// 안전하게 Long으로 변환
    private Long getLongValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}

