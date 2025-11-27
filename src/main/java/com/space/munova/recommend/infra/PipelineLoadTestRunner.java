package com.space.munova.recommend.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 파이프라인 부하 테스트용 Load Generator
 * <p>
 * ⚠️ 주의: 이 클래스는 API 레이어를 완전히 우회하고 RedisStreamProducer를 직접 호출합니다.
 * <p>
 * 목적:
 * - 버퍼 → 배치 스케줄러 → Redis Stream 파이프라인의 처리량을 테스트
 * - API 레이어 없이 내부 파이프라인만 순수하게 테스트
 * - "초당 20,000개의 로그를 생성하고 1초 대기하는 것을 10초 동안 반복"
 * - 즉, 파이프라인이 초당 20,000개의 로그를 견뎌낼 수 있는지 테스트
 * <p>
 * 동작 방식:
 * - 초당 20,000개 로그 생성 (API에 20,000개 요청이 오는 것과 비슷한 부하)
 * - 1초 대기
 * - 위 과정을 10초 동안 반복
 * - 총 200,000개 로그 생성 (20,000개 × 10초)
 * <p>
 * 사용법:
 * SPRING_PROFILES_ACTIVE=loadtest java -jar munova.jar
 * <p>
 * 또는 Docker:
 * docker run -e SPRING_PROFILES_ACTIVE=loadtest munova-api
 */
@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class PipelineLoadTestRunner implements CommandLineRunner {

    private final RedisStreamProducer redisStreamProducer;
    private final LogBatchBuffer logBuffer;

    @Qualifier("clusterRedisTemplate")
    private final RedisTemplate<String, Object> clusterRedisTemplate;

    // 부하 테스트 설정
    // VUS 20,000을 10초 동안 유지하는 부하 테스트

    /**
     * VUS (Virtual User) 20,000: 시뮬레이션할 가상 사용자 수
     * - 실제 스레드 20,000개를 생성하지 않고, 적은 수의 스레드로 부하를 생성합니다
     * - 각 스레드는 여러 VUS를 시뮬레이션하여 총 20,000 VUS의 부하를 만듭니다
     */
    private static final int VUS_COUNT = 30000;           // VUS (Virtual User) 개수

    /**
     * 실제 사용할 스레드 풀 크기
     * - 초당 30,000개를 생성하려면 여러 스레드가 필요합니다
     * - 스레드 1개로는 초당 약 9,000~10,000개 정도만 생성 가능합니다
     * - 스레드 4개 정도면 초당 30,000개 생성 가능합니다
     */
    private static final int THREAD_POOL_SIZE = 4; // 초당 30,000개 생성용

    /**
     * 초당 생성할 로그 수: 1초에 생성할 로그의 개수
     * - API에 초당 20,000개 요청이 오는 것과 비슷한 부하를 시뮬레이션합니다
     * - 파이프라인이 초당 20,000개를 견뎌낼 수 있는지 테스트합니다
     */
    private static final int LOGS_PER_SECOND = 30000;      // 초당 생성할 로그 수

    /**
     * 테스트 지속 시간: 부하 테스트를 실행할 시간 (초)
     * - 10초 동안 초당 20,000개씩 로그를 생성합니다
     * - 각 초마다 20,000개 생성 → 1초 대기 → 반복
     * - 총 200,000개 로그 생성 (20,000개 × 10초)
     */
    private static final int TEST_DURATION_SECONDS = 10;   // 테스트 지속 시간 (초)

    /**
     * Redis 연결 대기 시간: 테스트 시작 전 Redis 클러스터 연결이 완료될 때까지 대기하는 시간 (초)
     * - 애플리케이션 시작 시 Redis 클러스터 연결이 완료되지 않을 수 있으므로 대기
     * - 10초 정도면 Redis 클러스터 토폴로지 갱신이 완료될 것으로 예상
     */
    private static final int REDIS_CONNECTION_WAIT_SECONDS = 10;   // Redis 연결 대기 시간 (초)

    // 통계용 카운터
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    @Override
    public void run(String... args) {
        log.info("==========================================");
        log.info("파이프라인 부하 테스트 시작");
        log.info("==========================================");
        log.info("VUS (Virtual User) 개수: {}", VUS_COUNT);
        log.info("실제 스레드 풀 크기: {}", THREAD_POOL_SIZE);
        log.info("초당 생성할 로그 수: {}개", LOGS_PER_SECOND);
        log.info("테스트 지속 시간: {}초", TEST_DURATION_SECONDS);
        log.info("동작 방식: {}개 생성 → 1초 대기 → {}개 생성 → 1초 대기... ({}번 반복)",
                LOGS_PER_SECOND, LOGS_PER_SECOND, TEST_DURATION_SECONDS);
        log.info("==========================================");

        // Redis 클러스터 연결이 완료될 때까지 대기 및 연결 테스트
        log.info("Redis 클러스터 연결 확인 중...");
        int maxRetries = 30; // 최대 30번 시도 (30초)
        boolean connected = false;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                // 실제 Redis 연결 테스트 (여러 번 확인하여 확실하게)
                // 1. SET/GET/DEL 테스트
                String testKey = "__connection_test__" + System.currentTimeMillis();
                clusterRedisTemplate.opsForValue().set(testKey, "test");
                Object value = clusterRedisTemplate.opsForValue().get(testKey);
                if (!"test".equals(value)) {
                    throw new RuntimeException("Redis 값 검증 실패: 예상='test', 실제='" + value + "'");
                }
                clusterRedisTemplate.delete(testKey);

                // 2. 추가 검증: 연결이 안정적인지 확인하기 위해 한 번 더 테스트
                Thread.sleep(100); // 짧은 대기 후 재확인
                String testKey2 = "__connection_test2__" + System.currentTimeMillis();
                clusterRedisTemplate.opsForValue().set(testKey2, "test2");
                Object value2 = clusterRedisTemplate.opsForValue().get(testKey2);
                if (!"test2".equals(value2)) {
                    clusterRedisTemplate.delete(testKey2);
                    throw new RuntimeException("Redis 두 번째 값 검증 실패");
                }
                clusterRedisTemplate.delete(testKey2);

                connected = true;
                log.info("✅ Redis 클러스터 연결 확인 완료. (시도 횟수: {}/{})", i + 1, maxRetries);
                break;
            } catch (Exception e) {
                lastException = e;
                if (i < maxRetries - 1) {
                    log.info("Redis 클러스터 연결 시도 중... (시도 {}/{}, 에러: {})",
                            i + 1, maxRetries, e.getClass().getSimpleName() + ": " + e.getMessage());
                    try {
                        Thread.sleep(1000); // 1초 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Redis 연결 확인 중 인터럽트 발생", ie);
                        return;
                    }
                } else {
                    log.error("❌ Redis 클러스터 연결 확인 실패 (최대 시도 횟수 {}회 도달)", maxRetries);
                    log.error("마지막 에러: {}", lastException.getMessage(), lastException);
                }
            }
        }

        if (!connected) {
            log.error("==========================================");
            log.error("⚠️ Redis 클러스터 연결 실패로 인해 부하 테스트를 중단합니다.");
            log.error("Redis 클러스터가 정상적으로 실행 중인지 확인하세요.");
            log.error("에러 상세: {}", lastException != null ? lastException.getMessage() : "알 수 없는 오류");
            log.error("==========================================");
            return; // 연결 실패 시 테스트 중단
        }

        log.info("Redis 클러스터 연결 상태: 정상");
        log.info("==========================================");

        long startTime = System.currentTimeMillis();

        // 적은 수의 스레드 풀 사용 (실제 OS 스레드 20,000개 생성 방지)
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "pipeline-load-test-");
            t.setDaemon(false);
            return t;
        });

        // 각 스레드가 처리할 VUS 수 계산
        int vusPerThread = VUS_COUNT / THREAD_POOL_SIZE;
        int remainingVus = VUS_COUNT % THREAD_POOL_SIZE;

        CountDownLatch latch = new CountDownLatch(THREAD_POOL_SIZE);

        // 각 스레드에서 여러 VUS를 시뮬레이션하여 로그 생성
        for (int threadId = 0; threadId < THREAD_POOL_SIZE; threadId++) {
            final int finalThreadId = threadId;
            final int vusForThisThread = vusPerThread + (threadId < remainingVus ? 1 : 0);
            final int startVusId = threadId * vusPerThread + Math.min(threadId, remainingVus);

            executor.submit(() -> {
                try {
                    int localSuccess = 0;
                    int localFailure = 0;
                    int logIndex = 0;

                    // 이 스레드가 처리할 초당 로그 수 계산
                    int logsPerSecondForThisThread = LOGS_PER_SECOND / THREAD_POOL_SIZE;
                    if (finalThreadId < LOGS_PER_SECOND % THREAD_POOL_SIZE) {
                        logsPerSecondForThisThread++; // 나머지 분배
                    }

                    // 10초 동안 반복: 초당 20,000개 생성 → 1초 대기
                    for (int second = 0; second < TEST_DURATION_SECONDS; second++) {
                        long secondStartTime = System.currentTimeMillis();
                        int logsGeneratedThisSecond = 0;

                        // 1초 동안 로그 생성 (이 스레드가 담당하는 만큼)
                        while (logsGeneratedThisSecond < logsPerSecondForThisThread) {
                            try {
                                // 여러 VUS를 시뮬레이션하기 위해 VUS ID를 순환시킴
                                int vusId = startVusId + (logIndex % vusForThisThread);
                                Map<String, Object> logData = createLogData(vusId, logIndex);
                                
                                // 버퍼가 거의 가득 찬 경우 (90% 이상) 잠시 대기
                                int retryCount = 0;
                                int maxRetries = 100; // 최대 100번 재시도 (약 100ms)
                                
                                while (retryCount < maxRetries && logBuffer.isNearlyFull()) {
                                    Thread.sleep(1); // 1ms 대기
                                    retryCount++;
                                }
                                
                                // sendLog() 호출 (원래 로직 유지)
                                redisStreamProducer.sendLog(logData);
                                localSuccess++;
                                logIndex++;
                                logsGeneratedThisSecond++;

                                // 1초가 지났는데 아직 목표 개수에 도달하지 못했다면 중단
                                if (System.currentTimeMillis() - secondStartTime >= 1000) {
                                    break;
                                }
                            } catch (Exception e) {
                                localFailure++;
                                if (localFailure <= 5) {
                                    PipelineLoadTestRunner.log.warn("스레드 {} 로그 생성 실패: {}", finalThreadId, e.getMessage());
                                }
                            }
                        }

                        // 1초 대기 (다음 초까지)
                        long elapsed = System.currentTimeMillis() - secondStartTime;
                        if (elapsed < 1000) {
                            try {
                                Thread.sleep(1000 - elapsed);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }

                    successCount.addAndGet(localSuccess);
                    failureCount.addAndGet(localFailure);

                    log.debug("스레드 {} 완료: VUS={}, 성공={}, 실패={}", finalThreadId, vusForThisThread, localSuccess, localFailure);
                } catch (Exception e) {
                    log.error("스레드 {} 실행 중 오류 발생", finalThreadId, e);
                } finally {
                    latch.countDown();
                }
            });
        }

        // 모든 스레드 완료 대기 (최대 10초 + 여유시간)
        executor.shutdown();
        try {
            boolean finished = latch.await(TEST_DURATION_SECONDS + 5, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("일부 스레드가 {}초 내에 완료되지 않았습니다.", TEST_DURATION_SECONDS + 5);
            }

            // 남은 작업 완료 대기
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // 버퍼에 남은 로그가 모두 처리될 때까지 대기 (최대 30초)
            log.info("버퍼에 남은 로그 처리 대기 중...");
            int waitCount = 0;
            while (logBuffer.size() > 0 && waitCount < 30) {
                Thread.sleep(1000);
                waitCount++;
                if (waitCount % 5 == 0) {
                    log.info("버퍼 크기: {} (대기 중...)", logBuffer.size());
                }
            }
            if (logBuffer.size() > 0) {
                log.warn("버퍼에 {}개의 로그가 남아있습니다.", logBuffer.size());
            } else {
                log.info("버퍼의 모든 로그가 처리되었습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("대기 중 인터럽트 발생", e);
        }

        long actualEndTime = System.currentTimeMillis();
        long duration = actualEndTime - startTime;
        long totalLogs = successCount.get() + failureCount.get();
        double logsPerSecond = (totalLogs * 1000.0) / duration;

        // 결과 출력
        log.info("==========================================");
        log.info("파이프라인 부하 테스트 완료");
        log.info("==========================================");
        log.info("VUS 개수: {}", VUS_COUNT);
        log.info("실제 사용된 스레드 수: {}", THREAD_POOL_SIZE);
        log.info("목표 지속 시간: {}초", TEST_DURATION_SECONDS);
        log.info("실제 소요 시간: {}초 ({}ms)", duration / 1000, duration);
        log.info("생성된 로그 수: {}", totalLogs);
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failureCount.get());
        log.info("실제 처리량: 초당 {:.2f}건", logsPerSecond);
        log.info("==========================================");
        log.info("버퍼 → 배치 스케줄러 → Redis Stream 파이프라인 테스트 완료");
        log.info("Redis 전송 상태는 /actuator/metrics/redis.stream.* 엔드포인트에서 확인하세요.");
        log.info("==========================================");
    }

    /**
     * 테스트용 로그 데이터 생성
     * <p>
     * 실제 API와 동일한 형식으로 생성합니다:
     * - member_id: Long 타입 (1~10000) - RedisBatchScheduler에서 member_id % 10으로 스트림 키 결정
     * - action: 랜덤 액션 (view, like, cart, click, purchase)
     * - product_id: Long 타입 (1~100000)
     * <p>
     * 스트림 키 결정:
     * - member_id % 10으로 계산되어 user_action_stream_0~9에 분산 저장됩니다
     */
    private Map<String, Object> createLogData(int threadId, int logIndex) {
        Map<String, Object> log = new HashMap<>();

        // 랜덤 데이터 생성 (실제 API와 동일한 형식)
        // member_id는 Long 타입으로 생성 (RedisBatchScheduler에서 member_id % 10으로 스트림 키 결정)
        long memberId = ThreadLocalRandom.current().nextLong(1, 10001);  // 1~10000
        log.put("member_id", memberId);
        log.put("action", getRandomAction());
        log.put("product_id", ThreadLocalRandom.current().nextLong(1, 100001)); // 1~100000

        // 추가 메타데이터 (선택사항)
        log.put("timestamp", System.currentTimeMillis());
        log.put("thread_id", threadId);
        log.put("log_index", logIndex);

        return log;
    }

    /**
     * 랜덤 액션 선택
     */
    private String getRandomAction() {
        String[] actions = {"view", "like", "cart", "click", "purchase"};
        return actions[ThreadLocalRandom.current().nextInt(actions.length)];
    }
}

