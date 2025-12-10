package com.space.munova.product.infra.batch;

import com.space.munova.product.infra.batch.dto.ProductStatsSyncDto;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

///  1시간마다 좋아요, 판매량, 조회수를 각각의 상품별로 레디스에 저장해놓은 값을 가져와
/// 몽고, RDB, ES에 업데이트.
@Configuration
@RequiredArgsConstructor
public class ProductStatsSyncBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final ProductStatsReader productStatsReader;
    private final ProductStatsProcessor productStatsProcessor;
    private final ProductStatsWriter productStatsWriter;

    @Bean
    public Job syncProductStatsJob() {

        return new JobBuilder("syncProductStatsJob", jobRepository)
                .start(syncProductStatsStep())
                .build();
    }


    @Bean
    public Step syncProductStatsStep() {

        return new StepBuilder("syncProductStatsStep", jobRepository)
                .<Long, ProductStatsSyncDto>chunk(5000, transactionManager) ///5천개씩 배치
                .reader(productStatsReader)
                .processor(productStatsProcessor)
                .writer(productStatsWriter)
                .build();
    }

}
