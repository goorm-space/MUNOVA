package com.space.munova.product.infra.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.space.munova.product.application.product.command.event.*;
import com.space.munova.product.domain.enums.EventType;
import com.space.munova.product.domain.enums.OutboxStatus;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.ProductMongoDocument;
import com.space.munova.product.infra.mysql.ProductOutbox;
import com.space.munova.product.infra.mysql.ProductOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.SpringApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublishScheduler {

    private final ProductOutboxRepository productOutboxRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    /// 아웃박스 테이블에 저장된 대기상태의 메시지를 1초간격으로 쏴준다.
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void publishPendingEvents() {
        List<ProductOutbox> pendingEvents = productOutboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        if(pendingEvents.isEmpty()) {
            return;
        }

        for (ProductOutbox outbox : pendingEvents) {
            try {
                Object eventDto = convertValue(outbox.getEventType(), outbox.getEventValue());
                eventPublisher.publishEvent(eventDto);
                outbox.changePublishStatus();
                productOutboxRepository.save(outbox);

            } catch (Exception e) {

                outbox.changeFailedStatus();
                productOutboxRepository.save(outbox);
            }
        }
    }

    /// 아웃박스 테이블에 저장된 대기상태의 메시지를 10초간격으로 쏴준다.
    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void publishFailedEvents() {
        List<ProductOutbox> failedEvent = productOutboxRepository
                .findByStatusOrderByCreatedAtAsc(OutboxStatus.FAILED);

        if(failedEvent.isEmpty()) {
            return;
        }

        for (ProductOutbox outbox : failedEvent) {
            try {
                Object eventDto = convertValue(outbox.getEventType(), outbox.getEventValue());
                eventPublisher.publishEvent(eventDto);
                outbox.changePublishStatus();
                productOutboxRepository.save(outbox);

            } catch (Exception e) {

                outbox.changeFailedStatus();
                productOutboxRepository.save(outbox);
            }
        }
    }


    private Object convertValue(EventType eventType, String eventValue) throws JsonProcessingException {

        return switch (eventType) {
            case PRODUCT_LIKE, PRODUCT_DISLIKE ->
                    objectMapper.readValue(eventValue, ProductLikeEventDto.class);

            case SAVE_PRODUCT_SYNC_MONGO ->
                    objectMapper.readValue(eventValue, ProductMongoDocument.class);

            case SAVE_PRODUCT_SYNC_ES ->
                    objectMapper.readValue(eventValue, ProductEsDocument.class);

            case UPDATE_PRODUCT_SYNC_MONGO, UPDATE_PRODUCT_SYNC_ES ->
                    objectMapper.readValue(eventValue, ProductUpdateEventDto.class);

            case DELETE_PRODUCT_SYNC_MONGO, DELETE_PRODUCT_SYNC_ES ->
                    objectMapper.readValue(eventValue, ProductDocDeleteEventDto.class);

            case DELETE_PRODUCT_LIKE ->
                    objectMapper.readValue(eventValue, ProductDeleteEvenForLikeDto.class);

            case DELETE_PRODUCT_CART ->
                    objectMapper.readValue(eventValue, ProductDeleteEventForCartDto.class);

            default ->
                    throw new IllegalArgumentException("확인할 수 없는 타입 : " +  eventType);
        };
    }
}
