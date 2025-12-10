package com.space.munova.product.infra.scheduler;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductStatsSyncBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job syncProductStatsJob;

    @Scheduled(cron = "0 0 * * * *") //
    public void syncProductStats() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(syncProductStatsJob, jobParameters);

        } catch (Exception e) {
            log.error("상품 통계 동기화 배치 실행 실패", e);

        }
    }
}
