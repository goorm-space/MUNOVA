package com.space.munova.product.infra.mysql.adepter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.product.application.cart.command.port.CartOutboxCommandPort;
import com.space.munova.product.application.like.command.port.ProductLikeOutboxCommandPort;
import com.space.munova.product.application.product.command.event.ProductDeleteEvenForLikeDto;
import com.space.munova.product.application.product.command.event.ProductDeleteEventForCartDto;
import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.application.product.command.port.OutboxCommandPort;
import com.space.munova.product.infra.batch.dto.ProductStatsSyncDto;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.domain.enums.EventType;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.product.infra.mysql.ProductOutbox;
import com.space.munova.product.infra.mysql.ProductOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class OutboxCommandAdapter implements OutboxCommandPort,
        CartOutboxCommandPort,
        ProductLikeOutboxCommandPort
       {

    private final ProductOutboxRepository productOutboxRepository;
    private final ObjectMapper objectMapper;  // 벨류 json형식으로 변환을위한 객체

    @Override
    public void syncSaveEsEvent(ProductEsDocument event) {

        saveOutbox(EventType.SAVE_PRODUCT_SYNC_ES, event);
    }

    @Override
    public void syncSaveMongoEvent(ProductMongoDocument event) {

        saveOutbox(EventType.SAVE_PRODUCT_SYNC_MONGO, event);
    }

    @Override
    public void syncDeleteEsEvent(ProductDocDeleteEventDto event) {

        saveOutbox(EventType.DELETE_PRODUCT_SYNC_ES, event);
    }

    @Override
    public void syncDeleteMongoEvent(ProductDocDeleteEventDto event) {

        saveOutbox(EventType.DELETE_PRODUCT_SYNC_MONGO, event);
    }

    @Override
    public void syncUpdateEsEvent(ProductUpdateEventDto event) {

        saveOutbox(EventType.UPDATE_PRODUCT_SYNC_ES, event);
    }

    @Override
    public void syncUpdateMongoEvent(ProductUpdateEventDto event) {

        saveOutbox(EventType.UPDATE_PRODUCT_SYNC_MONGO, event);
    }


    @Override
    public void deleteCartEvent(ProductDeleteEventForCartDto event) {

        saveOutbox(EventType.DELETE_PRODUCT_CART, event);
    }

    @Override
    public void deleteLikeEvent(ProductDeleteEvenForLikeDto event) {

        saveOutbox(EventType.DELETE_PRODUCT_LIKE, event);
    }

//    @Override
//    public void syncUpdateStatsEsFailedEvent(ProductStatsSyncDto event) {
//        saveFailedOutbox(EventType.PRODUCT_STATS_SYNC_FAILED_ES, event);
//    }
//
//    @Override
//    public void syncUpdateStatsMongoFailedEvent(ProductStatsSyncDto event) {
//        saveFailedOutbox(EventType.PRODUCT_STATS_SYNC_FAILED_MONGO, event);
//    }

           @Override
    public void deleteLikeFailedEvent(ProductDeleteEvenForLikeDto event) {
        saveFailedOutbox(EventType.DELETE_PRODUCT_LIKE, event);
    }

    @Override
    public void deleteCartFailedEvent(ProductDeleteEventForCartDto event) {
        saveFailedOutbox(EventType.DELETE_PRODUCT_CART, event);
    }






    /// =================================================================///

    private void saveOutbox(EventType eventType, Object eventValue) {
        try {
            // JSON 변환
            String jsonValue = objectMapper.writeValueAsString(eventValue);

            ProductOutbox outbox = ProductOutbox.from(eventType, jsonValue);

            productOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {

            throw new RuntimeException("Outbox 저장 실패", e);
        }
    }



    private void saveFailedOutbox(EventType eventType, Object eventValue) {
        try {
            // JSON 변환
            String jsonValue = objectMapper.writeValueAsString(eventValue);

            ProductOutbox outbox = ProductOutbox.from(eventType, jsonValue);
            outbox.changeFailedStatus();

            productOutboxRepository.save(outbox);
        } catch (JsonProcessingException e) {

            throw new RuntimeException("Outbox 저장 실패", e);
        }
    }


}
