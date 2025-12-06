package com.space.munova.product.application.product.query.dto;

import com.space.munova.product.infra.mongo.ProductMongoDocument;

public record ColorOptionDto (Long colorOptionId,
                              String OptionType,
                              String color){

}
