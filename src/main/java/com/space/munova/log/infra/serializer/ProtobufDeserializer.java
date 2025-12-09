package com.space.munova.log.infra.serializer;

import com.google.protobuf.InvalidProtocolBufferException;
import com.space.munova.log.infra.dto.proto.UserActionLogProto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

/**
 * Kafka Consumer용 Protobuf Deserializer
 * byte[]를 UserActionLogProto.UserActionLog로 역직렬화
 */
@Slf4j
public class ProtobufDeserializer implements Deserializer<UserActionLogProto.UserActionLog> {

    @Override
    public UserActionLogProto.UserActionLog deserialize(String topic, byte[] data) {
        if (data == null) {
            return null;
        }
        try {
            return UserActionLogProto.UserActionLog.parseFrom(data);
        } catch (InvalidProtocolBufferException e) {
            log.error("Protobuf 역직렬화 실패: topic={}, error={}", topic, e.getMessage(), e);
            throw new SerializationException("Error deserializing protobuf message", e);
        }
    }
}

