package com.space.munova.product.infra.mongo;


import com.space.munova.product.domain.enums.OutboxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document(collection = "productOutbox")
//@CompoundIndex(name = "idx_complex_category_option_created_desc_id_desc"
//        , def = "{'categoryId': 1, 'optionIds': 1, 'createdAt': -1, '_id': -1}")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductOutboxMongoDocument {

    @Id
    private ObjectId id;

    /// 이벤트 타입
    @Field("eventType")
    private String eventType;

    /// 이벤트 벨류
    @Field("eventValue")
    private Object eventValue;

    /// 메시지 발생 상태. false 발행 x , true 발행 o
    @Field("status")
    private String status;

    /// 생성일자.
    @Field("createdAt")
    private LocalDateTime createdAt;

    public static ProductOutboxMongoDocument from(String eventType, Object eventValue) {
        return ProductOutboxMongoDocument.builder()
                .eventType(eventType)
                .eventValue(eventValue)
                .createdAt(LocalDateTime.now())
                .status(OutboxStatus.PENDING.getValue())
                .build();
    }

}
