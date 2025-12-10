package com.space.munova.product.application.product.command.port;

import com.space.munova.product.application.product.command.event.ProductDeleteEvenForLikeDto;
import com.space.munova.product.application.product.command.event.ProductDeleteEventForCartDto;
import com.space.munova.product.application.product.command.event.ProductDocDeleteEventDto;
import com.space.munova.product.application.product.command.event.ProductUpdateEventDto;
import com.space.munova.product.infra.batch.dto.ProductStatsSyncDto;
import com.space.munova.product.infra.elasticsearch.ProductEsDocument;
import com.space.munova.product.infra.mongo.ProductMongoDocument;

public interface OutboxCommandPort {

    ///  아웃박스 테이블 저장용.
    void syncSaveEsEvent(ProductEsDocument event);
    void syncSaveMongoEvent(ProductMongoDocument event);
    void syncDeleteEsEvent(ProductDocDeleteEventDto event);
    void syncDeleteMongoEvent(ProductDocDeleteEventDto event);
    void syncUpdateEsEvent(ProductUpdateEventDto event);
    void syncUpdateMongoEvent(ProductUpdateEventDto event);
    void deleteCartEvent(ProductDeleteEventForCartDto event);
    void deleteLikeEvent(ProductDeleteEvenForLikeDto event);

//    void syncUpdateStatsEsFailedEvent(ProductStatsSyncDto dto);
//    void syncUpdateStatsMongoFailedEvent(ProductStatsSyncDto dto);
}
