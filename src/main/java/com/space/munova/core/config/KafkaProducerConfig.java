package com.space.munova.core.config;

import com.space.munova.log.infra.dto.proto.UserActionLogProto;
import com.space.munova.log.infra.serializer.ProtobufSerializer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    /// kafka broker 설정 정보
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // TODO: Protobuf 적용
    // TODO: BUFFER_MEMORY_CONFIG, LINGER_MS_CONFIG, MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION 튜닝 해보기..?
    @Bean
    public ProducerFactory<String, UserActionLogProto.UserActionLog> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers); /// kafka broker list
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class); /// key는 문자열 직렬화
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ProtobufSerializer.class); /// value는 Protobuf 직렬화 (JSON 대비 비용 절감)
        configProps.put(ProducerConfig.ACKS_CONFIG, "all"); /// 데이터 안전성 검사 메시지 유실 ㄴ
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3); /// 실패시 3번 재시도
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true); /// 중복 메시지 방지 (재시도 시에도 중복 메시지 안들어가도록)
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 65536); /// 성능 -> 64KB까지 모아서 한번에 전송 (16~64KB 사용)
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 134217728); /// Producer 내부 버퍼로 128MB 사용 (32~512MB 사용)
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 3); /// 배치 전송 대기 시간 (1~5ms)
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy"); /// 압축 설정
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5); /// 동시에 전송할 수 있는 요청 수
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30000); /// 요청 타임아웃
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000); /// 전송 응답 타임아웃
        configProps.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, 10000); /// 버퍼 블로킹 최대 대기 시간 (10초) -> error "PRODUCT BUFFER IS FULL"
        configProps.put(ProducerConfig.METADATA_MAX_AGE_CONFIG, 300000); /// 메타데이터 최대 유지 시간 (5분) -> 브로커 클러스터 정보 (파티션 리더 등) 를 5분마다 갱신

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    public KafkaTemplate<String, UserActionLogProto.UserActionLog> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}

