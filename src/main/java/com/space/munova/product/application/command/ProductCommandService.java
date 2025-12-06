package com.space.munova.product.application.command;

import com.space.munova.product.infra.elasticsearch.command.ProductEsCommandRepo;
import com.space.munova.product.infra.mongo.command.ProductMongoCommandRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductCommandService {

    private final ProductMongoCommandRepo productMongoCommandRepo;
    private final ProductEsCommandRepo productEsCommandRepo;
    private ApplicationEventPublisher eventPublisher;

}
