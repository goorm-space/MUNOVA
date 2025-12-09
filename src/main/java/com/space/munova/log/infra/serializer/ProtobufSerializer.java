package com.space.munova.log.infra.serializer;

import com.space.munova.log.infra.dto.proto.UserActionLogProto;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serializer;

@Slf4j
public class ProtobufSerializer implements Serializer<UserActionLogProto.UserActionLog> {

    @Override
    public byte[] serialize(String topic, UserActionLogProto.UserActionLog data) {
        if (data == null) {
            return null;
        }
        try {
            return data.toByteArray();
        } catch (Exception e) {
            log.error("Protobuf 직렬화 실패: topic={}, error={}", topic, e.getMessage(), e);
            throw new RuntimeException("Protobuf serialization failed", e);
        }
    }
}

