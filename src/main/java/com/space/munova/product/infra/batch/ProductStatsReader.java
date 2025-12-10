package com.space.munova.product.infra.batch;

import com.space.munova.product.domain.Repository.ProductRepository;
import com.space.munova.product.infra.redis.command.ProductStatsRedisDataCommandRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProductStatsReader implements ItemReader<Long> {

    private final ProductRepository productRepository;
    private final ProductStatsRedisDataCommandRepo productStatsRedisDataCommandRepo;

    private static final int BATCH_SIZE = 5000;
    private List<Long> productIds;
    private int cur = 0;

    @Override
    public Long read() throws Exception {

        if(productIds == null || cur >= productIds.size()) {
            productIds = getNext();
            cur = 0;

            if(productIds == null || productIds.isEmpty()) {
                return null;
            }
        }

        return productIds.get(cur++);
    }

    private List<Long> getNext() {

        /// 현재 할당된 아이디 가져옴.
        Long allocatedProductId = productStatsRedisDataCommandRepo.findAllocatedProductId();
        Long endProductId = allocatedProductId + BATCH_SIZE;

        List<Long> batchIds = productRepository.findProductIdsGtIdLimitBatchSize(allocatedProductId, endProductId);

        /// 더없을경우 다시 0 으로 초기화
        if(batchIds.isEmpty()) {
            productStatsRedisDataCommandRepo.resetAllocatedProductId();
            return null;
        }

        return batchIds;
    }
}
