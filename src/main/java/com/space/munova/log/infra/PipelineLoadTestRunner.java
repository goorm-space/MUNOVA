package com.space.munova.log.infra;

import com.space.munova.log.infra.dto.proto.UserActionLogProto;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Component
@Profile("loadtest")
@RequiredArgsConstructor
public class PipelineLoadTestRunner implements CommandLineRunner {

    private final UserActionKafkaProducer kafkaProducer;
    private final KafkaTemplate<String, UserActionLogProto.UserActionLog> kafkaTemplate;
    private final MeterRegistry meterRegistry;
    
    // Topic 초기화가 이미 실행되었는지 추적 (애플리케이션 재시작 시 중복 초기화 방지)
    private static volatile boolean topicInitialized = false;

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    // 부하 테스트 설정 - 초당 30만개, 1분(60초) 테스트용
    // 목표: 300,000 req/s × 60초 = 18,000,000개 (1,800만개)
    private static final int VUS_COUNT = 600000;            // VUS (Virtual User) 개수 (초당 30만명 테스트용)
    private static final int THREAD_POOL_SIZE = 30;         // 스레드 풀 크기 (초당 30만개 처리용)
    private static final int LOGS_PER_SECOND = 300000;      // 초당 생성할 로그 수 (30만개)
    private static final int TEST_DURATION_SECONDS = 60;    // 테스트 지속 시간 (초) - 60초 (300,000 * 60 = 18,000,000)
    private static final int KAFKA_CONNECTION_WAIT_SECONDS = 10;   // Kafka 연결 대기 시간 (초)

    // 단계적 부하 증가 설정 (선택적 - 한계 탐색용)
    // true로 설정하면 단계적으로 부하를 증가시켜 한계점을 찾습니다
    private static final boolean USE_RAMP_UP = true;      // 단계적 부하 증가 사용 여부
    // 부하 테스트 프로파일: 1분(10k) → 증가(10k→300k) → 10초(300k) → 감소(300k→10k)
    // 단계: [10k(60s)] → [50k(30s), 100k(30s), 150k(30s), 200k(30s), 250k(30s), 300k(30s)] → [300k(10s)] → [250k(30s), 200k(30s), 150k(30s), 100k(30s), 50k(30s), 10k(30s)]
    private static final int[] RAMP_UP_LOGS_PER_SECOND = {
            10000,      // 1단계: 초당 1만개 (1분)
            50000,      // 2단계: 초당 5만개 (증가 시작)
            100000,     // 3단계: 초당 10만개
            200000,     // 5단계: 초당 20만개
            300000,     // 7단계: 초당 30만개 (피크 도달)
            300000,     // 8단계: 초당 30만개 유지 (10초)
            200000,     // 10단계: 초당 20만개
            100000,     // 12단계: 초당 10만개
            50000,      // 13단계: 초당 5만개
            10000       // 14단계: 초당 1만개 (원래대로)
    };
    private static final int[] RAMP_UP_DURATION_SECONDS = {
            10,         // 1단계: 60초 (1분)
            15,         // 2단계: 30초
            15,         // 3단계: 30초
            15,         // 5단계: 30초
            15,         // 7단계: 30초
            10,         // 8단계: 10초 (피크 유지)
            15,         // 10단계: 30초
            15,         // 12단계: 30초
            15,         // 13단계: 30초
            15          // 14단계: 30초
    };

    // Topic 설정 (3 Broker Cluster용)
    private static final String TOPIC_USER_ACTION_LOG = "user_action_log";
    private static final String TOPIC_DLQ = "dlq_user_action";
    private static final int TOPIC_PARTITIONS = 10;  // 파티션 수 (10개 파티션)
    private static final short TOPIC_REPLICATION_FACTOR = 3;  // 복제 팩터 (3 Broker Cluster용)

    // Topic 초기화 여부 (true로 설정하면 부하 테스트 시작 전에만 Topic 삭제/재생성 함)
    // false로 설정하면 기존 Topic의 메시지를 유지 (이전 테스트 데이터가 남아있음)


    // ⚠️ 주의: true로 설정하면 테스트 시작 전에만 모든 메시지가 삭제됩니다!
    // ⚠️ 테스트 중간이나 끝에는 초기화하지 않습니다 (Consumer 에러 방지)
    private static final boolean RESET_TOPICS = true;

    // 통계용 카운터
    private final AtomicLong successCount = new AtomicLong(0);
    private final AtomicLong failureCount = new AtomicLong(0);

    @Override
    public void run(String... args) {
        // 부하 테스트를 비동기로 실행하여 run() 메서드가 즉시 종료되도록 함
        // 이렇게 하면 애플리케이션이 계속 실행됨
        CompletableFuture.runAsync(() -> {
            try {
                runLoadTest();
            } catch (Exception e) {
                log.error("부하 테스트 실행 중 예외 발생", e);
            }
        });
        
        log.info("부하 테스트가 백그라운드에서 시작되었습니다. 애플리케이션은 계속 실행됩니다.");
    }
    
    private void runLoadTest() {
        log.info("==========================================");
        log.info("Kafka 파이프라인 부하 테스트 시작 (실제 프로덕션 로직 사용)");
        log.info("==========================================");
        log.info("VUS (Virtual User) 개수: {}", VUS_COUNT);
        log.info("실제 스레드 풀 크기: {}", THREAD_POOL_SIZE);

        if (USE_RAMP_UP) {
            log.info("부하 테스트 모드: 단계적 부하 증가/감소 (Ramp-up/Ramp-down)");
            int totalDuration = 0;
            long totalExpectedLogs = 0;
            log.info("부하 테스트 프로파일:");
            for (int i = 0; i < RAMP_UP_LOGS_PER_SECOND.length; i++) {
                int stageLogs = RAMP_UP_LOGS_PER_SECOND[i];
                int stageDuration = RAMP_UP_DURATION_SECONDS[i];
                totalDuration += stageDuration;
                totalExpectedLogs += (long) stageLogs * stageDuration;
                log.info("  단계 {}: 초당 {}개 × {}초 = {}개",
                        i + 1, stageLogs, stageDuration, stageLogs * stageDuration);
            }
            log.info("총 예상 지속 시간: {}초 (약 {}분)", totalDuration, totalDuration / 60);
            log.info("총 예상 로그 수: {}개", totalExpectedLogs);
        } else {
            log.info("부하 테스트 모드: 고정 부하");
            log.info("초당 생성할 로그 수: {}개", LOGS_PER_SECOND);
            log.info("테스트 지속 시간: {}초", TEST_DURATION_SECONDS);
            log.info("예상 총 로그 수: {}개 ({} * {})", LOGS_PER_SECOND * TEST_DURATION_SECONDS, LOGS_PER_SECOND, TEST_DURATION_SECONDS);
            log.info("동작 방식: {}개 생성 → 1초 대기 → {}개 생성 → 1초 대기... ({}번 반복)",
                    LOGS_PER_SECOND, LOGS_PER_SECOND, TEST_DURATION_SECONDS);
        }

        log.info("전송 방식: UserActionKafkaProducer.sendLog() 사용 (비동기 전송)");
        log.info("==========================================");

        // Kafka 연결 확인
        log.info("Kafka 연결 확인 중...");
        int kafkaConnectionMaxRetries = 30; // 최대 30번 시도 (30초)
        boolean connected = false;
        Exception lastException = null;

        for (int i = 0; i < kafkaConnectionMaxRetries; i++) {
            try {
                // Kafka 연결 테스트 (metadata 조회)
                kafkaTemplate.getProducerFactory().createProducer().partitionsFor("user_action_log");
                connected = true;
                log.info("✅ Kafka 연결 확인 완료. (시도 횟수: {}/{})", i + 1, kafkaConnectionMaxRetries);
                break;
            } catch (Exception e) {
                lastException = e;
                if (i < kafkaConnectionMaxRetries - 1) {
                    log.info("Kafka 연결 시도 중... (시도 {}/{}, 에러: {})",
                            i + 1, kafkaConnectionMaxRetries, e.getClass().getSimpleName() + ": " + e.getMessage());
                    try {
                        Thread.sleep(1000); // 1초 대기
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Kafka 연결 확인 중 인터럽트 발생", ie);
                        return;
                    }
                } else {
                    log.error("❌ Kafka 연결 확인 실패 (최대 시도 횟수 {}회 도달)", kafkaConnectionMaxRetries);
                    log.error("마지막 에러: {}", lastException.getMessage(), lastException);
                }
            }
        }

        if (!connected) {
            log.error("==========================================");
            log.error("⚠️ Kafka 연결 실패로 인해 부하 테스트를 중단합니다.");
            log.error("Kafka가 정상적으로 실행 중인지 확인하세요.");
            log.error("에러 상세: {}", lastException != null ? lastException.getMessage() : "알 수 없는 오류");
            log.error("==========================================");
            return; // 연결 실패 시 테스트 중단
        }

        log.info("Kafka 연결 상태: 정상");
        log.info("==========================================");

        // Kafka Topic 초기화 (테스트 전 깔끔한 상태로 시작)
        // 주의: Topic 삭제/재생성은 메타데이터 갱신 시간이 필요하므로 선택적으로 실행
        // ⚠️ Topic 삭제/재생성 시 Producer가 강제 종료될 수 있으므로 주의
        // ⚠️ 애플리케이션 재시작 시 중복 초기화 방지 (Topic이 이미 비어있으면 초기화하지 않음)
        if (RESET_TOPICS && !topicInitialized) {
            // Topic이 이미 비어있는지 확인 (재시작 시 중복 초기화 방지)
            try {
                long messageCount = getTopicMessageCount(TOPIC_USER_ACTION_LOG);
                if (messageCount == 0) {
                    log.info("Kafka Topic이 이미 비어있습니다. 초기화를 건너뜁니다. (재시작 감지)");
                    topicInitialized = true;
                } else {
                    log.info("Kafka Topic에 메시지가 {}건 있습니다. 초기화를 진행합니다.", messageCount);
                }
            } catch (Exception e) {
                log.warn("Topic 메시지 수 확인 실패. 초기화를 진행합니다: {}", e.getMessage());
            }
        }
        
        if (RESET_TOPICS && !topicInitialized) {
            log.info("Kafka Topic 초기화 시작...");
            try {
                // Topic 삭제 전 Producer 리셋 (Idempotence Producer ID 리셋을 위해)
                log.info("Topic 삭제 전 Producer 리셋 중...");
                try {
                    kafkaTemplate.flush(); // 남은 메시지 전송 완료
                    log.info("✅ Producer flush 완료");
                } catch (Exception e) {
                    log.warn("Producer flush 중 오류 (무시): {}", e.getMessage());
                }

                // Topic이 이미 존재하면 삭제하지 않고 재사용 (메타데이터 갱신 시간 절약)
                resetKafkaTopics();
                topicInitialized = true; // 초기화 완료 표시
                log.info("✅ Kafka Topic 초기화 완료");

                // Topic 초기화 후 Producer가 메타데이터를 새로 가져올 수 있도록 충분한 대기
                // 3 Broker Cluster 환경에서는 메타데이터 전파에 더 많은 시간이 필요
                log.info("Producer 메타데이터 갱신 대기 중... (3 Broker Cluster 안정화 대기)");
                Thread.sleep(10000); // 10초 대기 (3 Broker Cluster 안정화 - 증가, Idempotence Producer ID 재할당 대기)

                // Producer 메타데이터 강제 갱신 (Topic 정보 조회로 메타데이터 갱신 유도)
                // Idempotence Producer는 새로운 Producer ID를 받기 위해 메타데이터 갱신 필요
                try {
                    // Topic 메타데이터를 조회하여 Producer가 새로운 메타데이터를 가져오도록 유도
                    kafkaTemplate.getProducerFactory().createProducer().partitionsFor(TOPIC_USER_ACTION_LOG);
                    log.info("✅ Producer 메타데이터 강제 갱신 완료");
                } catch (Exception e) {
                    log.warn("Producer 메타데이터 갱신 중 오류 (재시도): {}", e.getMessage());
                    Thread.sleep(5000); // 추가 대기 후 재시도
                    try {
                        kafkaTemplate.getProducerFactory().createProducer().partitionsFor(TOPIC_USER_ACTION_LOG);
                        log.info("✅ Producer 메타데이터 재갱신 완료");
                    } catch (Exception e2) {
                        log.warn("Producer 메타데이터 재갱신 실패 (무시): {}", e2.getMessage());
                    }
                }

                // Producer 재연결을 위해 flush 호출
                try {
                    kafkaTemplate.flush();
                    log.info("✅ Producer flush 완료");
                } catch (Exception e) {
                    log.warn("Producer flush 중 오류 (무시): {}", e.getMessage());
                }

                // 추가 안정화 대기 (3 Broker Cluster + Idempotence Producer ID 재할당 대기)
                Thread.sleep(5000); // 5초 추가 대기 (증가)
                log.info("✅ Kafka 클러스터 안정화 완료");
            } catch (Exception e) {
                log.warn("⚠️ Kafka Topic 초기화 실패: {}. 기존 Topic 사용", e.getMessage());
                // 초기화 실패해도 테스트는 계속 진행
            }
        } else if (topicInitialized) {
            log.info("Kafka Topic 초기화 건너뜀 (이미 초기화됨). 기존 Topic 사용");
        } else {
            log.info("Kafka Topic 초기화 건너뜀 (RESET_TOPICS=false). 기존 Topic 사용");
        }
        log.info("==========================================");

        long startTime = System.currentTimeMillis();

        // 적은 수의 스레드 풀 사용 (실제 OS 스레드 20,000개 생성 방지)
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r, "kafka-pipeline-load-test-");
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

                    // 단계적 부하 증가 또는 고정 부하
                    if (USE_RAMP_UP) {
                        // 단계적 부하 증가 모드
                        AtomicLong localSuccessAtomic = new AtomicLong(localSuccess);
                        AtomicLong localFailureAtomic = new AtomicLong(localFailure);
                        AtomicLong logIndexAtomic = new AtomicLong(logIndex);
                        runRampUpLoadTest(finalThreadId, vusForThisThread, startVusId,
                                localSuccessAtomic, localFailureAtomic, logIndexAtomic);
                        localSuccess = (int) localSuccessAtomic.get();
                        localFailure = (int) localFailureAtomic.get();
                        logIndex = (int) logIndexAtomic.get();
                    } else {
                        // 고정 부하 모드 (초당 30만개, 1분)
                        int logsPerSecondForThisThread = LOGS_PER_SECOND / THREAD_POOL_SIZE;
                        if (finalThreadId < LOGS_PER_SECOND % THREAD_POOL_SIZE) {
                            logsPerSecondForThisThread++; // 나머지 분배
                        }

                        // 60초 동안 반복: 초당 300,000개 생성 → 정확히 1초 동안만 생성 (총 18,000,000개 목표)
                        // 3 Broker Cluster 환경에서 테스트
                        for (int second = 0; second < TEST_DURATION_SECONDS; second++) {
                            long secondStartTime = System.currentTimeMillis();
                            int logsGeneratedThisSecond = 0;
                            long targetEndTime = secondStartTime + 1000; // 정확히 1초 후

                            // 정확히 1초 동안만 메시지 생성 (시간 제한 추가)
                            while (logsGeneratedThisSecond < logsPerSecondForThisThread
                                    && System.currentTimeMillis() < targetEndTime) {
                                try {
                                    // 여러 VUS를 시뮬레이션하기 위해 VUS ID를 순환시킴
                                    int vusId = startVusId + (logIndex % vusForThisThread);
                                    Map<String, Object> logData = createLogData(vusId, logIndex);

                                    // 실제 프로덕션 로직 사용: UserActionKafkaProducer의 비동기 전송
                                    kafkaProducer.sendLog(logData);
                                    localSuccess++;
                                    logIndex++;
                                    logsGeneratedThisSecond++;

                                } catch (Exception e) {
                                    localFailure++;
                                    if (localFailure <= 5) {
                                        PipelineLoadTestRunner.log.warn("스레드 {} 로그 생성 실패: {}", finalThreadId, e.getMessage());
                                    }
                                }
                            }

                            // 각 초마다 실제 생성량 로깅
                            long elapsed = System.currentTimeMillis() - secondStartTime;
                            log.info("스레드 {} - {}초: 목표={}, 실제={}, 소요시간={}ms",
                                    finalThreadId, second + 1, logsPerSecondForThisThread,
                                    logsGeneratedThisSecond, elapsed);

                            // 정확히 1초가 될 때까지 대기
                            long remainingTime = targetEndTime - System.currentTimeMillis();
                            if (remainingTime > 0) {
                                try {
                                    Thread.sleep(remainingTime);
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            } else if (remainingTime < 0) {
                                // 시간이 초과된 경우 경고
                                log.warn("스레드 {} - {}초: 목표 시간 초과 ({}ms 초과)",
                                        finalThreadId, second + 1, Math.abs(remainingTime));
                            }
                        } // for 루프 종료
                    } // else 블록 종료

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
            boolean finished = latch.await(TEST_DURATION_SECONDS + 10, TimeUnit.SECONDS);
            if (!finished) {
                log.warn("일부 스레드가 {}초 내에 완료되지 않았습니다.", TEST_DURATION_SECONDS + 5);
            }

            // 남은 작업 완료 대기
            executor.awaitTermination(5, TimeUnit.SECONDS);

            // Kafka 전송 완료 대기 및 flush
            log.info("Kafka 비동기 전송 완료 대기 중...");

            // UserActionKafkaProducer가 CompletableFuture.runAsync()를 사용하므로
            // 모든 비동기 작업이 완료될 때까지 충분한 시간 대기
            // 부하 테스트 시 대량 메시지 처리 시간 고려하여 대기 시간 증가
            Thread.sleep(15000); // 15초 대기 (비동기 작업 완료 대기 - 증가)

            // Kafka Producer의 모든 전송 완료를 위해 flush 호출
            try {
                kafkaTemplate.flush();
                log.info("✅ Kafka Producer flush 완료");
            } catch (Exception e) {
                log.warn("Kafka Producer flush 중 오류 발생: {}", e.getMessage());
            }

            // 추가 대기 시간 (Kafka 버퍼에 있는 메시지 전송 완료 대기)
            // 부하 테스트 시 버퍼에 대량 메시지가 있을 수 있으므로 대기 시간 증가
            Thread.sleep(10000); // 10초 추가 대기 (5초 → 10초 증가)
            log.info("✅ 모든 Kafka 비동기 전송 완료 대기 종료");

            // 메트릭 확인 및 Kafka Topic 메시지 확인
            checkMetricsAndKafkaMessages();

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
        log.info("Kafka 파이프라인 부하 테스트 완료");
        log.info("==========================================");
        log.info("VUS 개수: {}", VUS_COUNT);
        log.info("실제 사용된 스레드 수: {}", THREAD_POOL_SIZE);
        log.info("목표 지속 시간: {}초", TEST_DURATION_SECONDS);
        log.info("실제 소요 시간: {}초 ({}ms)", duration / 1000, duration);
        log.info("생성된 로그 수: {}", totalLogs);
        log.info("성공: {}", successCount.get());
        log.info("실패: {}", failureCount.get());
        log.info("실제 처리량: 초당 {}건", String.format("%.2f", logsPerSecond));
        log.info("==========================================");
        log.info("Kafka Producer → user_action_log Topic 파이프라인 테스트 완료");
        log.info("Kafka 전송 상태는 /actuator/metrics/kafka.producer.* 엔드포인트에서 확인하세요.");
        log.info("==========================================");
    }

    private Map<String, Object> createLogData(int vusId, int logIndex) {
        Map<String, Object> log = new HashMap<>();

        // 랜덤 데이터 생성 (실제 API와 동일한 형식)
        long memberId = ThreadLocalRandom.current().nextLong(1, 10001);  // 1~10000
        String eventType = getRandomEventType();
        long productId = ThreadLocalRandom.current().nextLong(1, 100001); // 1~100000

        // Kafka Producer가 기대하는 형식으로 데이터 생성
        log.put("event_type", eventType);
        log.put("service", "product");
        log.put("member_id", memberId);
        log.put("data", Map.of(
                "product_id", productId
        ));

        // 추가 메타데이터 (선택사항)
        log.put("timestamp", System.currentTimeMillis());
        log.put("vus_id", vusId);
        log.put("log_index", logIndex);

        return log;
    }

    private String getRandomEventType() {
        String[] eventTypes = {"product_detail_view", "product_like", "product_add_cart", "product_click", "product_purchase"};
        return eventTypes[ThreadLocalRandom.current().nextInt(eventTypes.length)];
    }

    /**
     * 단계적 부하 증가 테스트 실행
     */
    private void runRampUpLoadTest(int threadId, int vusForThisThread, int startVusId,
                                   AtomicLong localSuccess, AtomicLong localFailure, AtomicLong logIndex) {
        int totalDuration = 0;
        for (int stage = 0; stage < RAMP_UP_LOGS_PER_SECOND.length; stage++) {
            int stageLogsPerSecond = RAMP_UP_LOGS_PER_SECOND[stage];
            int stageDuration = RAMP_UP_DURATION_SECONDS[stage];

            int logsPerSecondForThisThread = stageLogsPerSecond / THREAD_POOL_SIZE;
            if (threadId < stageLogsPerSecond % THREAD_POOL_SIZE) {
                logsPerSecondForThisThread++; // 나머지 분배
            }

            log.info("스레드 {} - 단계 {} 시작: 초당 {}개, 지속시간 {}초",
                    threadId, stage + 1, stageLogsPerSecond, stageDuration);

            for (int second = 0; second < stageDuration; second++) {
                long secondStartTime = System.currentTimeMillis();
                int logsGeneratedThisSecond = 0;
                long targetEndTime = secondStartTime + 1000; // 정확히 1초 후

                // 정확히 1초 동안만 메시지 생성 (시간 제한 추가)
                while (logsGeneratedThisSecond < logsPerSecondForThisThread
                        && System.currentTimeMillis() < targetEndTime) {
                    try {
                        int vusId = startVusId + (logIndex.intValue() % vusForThisThread);
                        Map<String, Object> logData = createLogData(vusId, logIndex.intValue());

                        kafkaProducer.sendLog(logData);
                        localSuccess.incrementAndGet();
                        logIndex.incrementAndGet();
                        logsGeneratedThisSecond++;

                    } catch (Exception e) {
                        localFailure.incrementAndGet();
                        if (localFailure.get() <= 5) {
                            log.warn("스레드 {} 로그 생성 실패: {}", threadId, e.getMessage());
                        }
                    }
                }

                long elapsed = System.currentTimeMillis() - secondStartTime;
                if (second % 30 == 0) { // 30초마다 로깅
                    log.info("스레드 {} - 단계 {} - {}초: 목표={}, 실제={}, 소요시간={}ms",
                            threadId, stage + 1, second + 1, logsPerSecondForThisThread,
                            logsGeneratedThisSecond, elapsed);
                }

                // 정확히 1초가 될 때까지 대기
                long remainingTime = targetEndTime - System.currentTimeMillis();
                if (remainingTime > 0) {
                    try {
                        Thread.sleep(remainingTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                } else if (remainingTime < 0) {
                    // 시간이 초과된 경우 경고
                    log.warn("스레드 {} - 단계 {} - {}초: 목표 시간 초과 ({}ms 초과)",
                            threadId, stage + 1, second + 1, Math.abs(remainingTime));
                }
            }

            totalDuration += stageDuration;
            log.info("스레드 {} - 단계 {} 완료: 총 {}초 경과", threadId, stage + 1, totalDuration);
        }
    }

    /**
     * 메트릭 확인 및 Kafka Topic 메시지 확인
     */
    private void checkMetricsAndKafkaMessages() {
        log.info("==========================================");
        log.info("메트릭 및 Kafka Topic 확인");
        log.info("==========================================");

        // 1. 메트릭 확인
        try {
            Counter successCounter = meterRegistry.find("kafka.producer.send.success")
                    .tag("component", "kafka_producer")
                    .tag("topic", TOPIC_USER_ACTION_LOG)
                    .counter();

            Counter failureCounter = meterRegistry.find("kafka.producer.send.failure")
                    .tag("component", "kafka_producer")
                    .tag("topic", TOPIC_USER_ACTION_LOG)
                    .counter();

            Counter dlqCounter = meterRegistry.find("kafka.producer.send.dlq")
                    .tag("component", "kafka_producer")
                    .tag("topic", TOPIC_DLQ)
                    .counter();

            if (successCounter != null) {
                double successCount = successCounter.count();
                log.info("✅ Kafka Producer 성공 메트릭: {}건", successCount);
            } else {
                log.warn("⚠️ Kafka Producer 성공 메트릭을 찾을 수 없습니다.");
            }

            if (failureCounter != null) {
                double failureCount = failureCounter.count();
                log.info("❌ Kafka Producer 실패 메트릭: {}건", failureCount);
            } else {
                log.warn("⚠️ Kafka Producer 실패 메트릭을 찾을 수 없습니다.");
            }

            if (dlqCounter != null) {
                double dlqCount = dlqCounter.count();
                log.info("📦 DLQ 전송 메트릭: {}건", dlqCount);
            }

        } catch (Exception e) {
            log.error("메트릭 확인 중 오류 발생: {}", e.getMessage(), e);
        }

        // 2. Kafka Topic 메시지 수 확인 및 파티션별 상세 정보
        try {
            log.info("------------------------------------------");
            log.info("Kafka Topic '{}' 파티션별 메시지 수 확인", TOPIC_USER_ACTION_LOG);
            log.info("------------------------------------------");

            Map<Integer, Long> partitionMessageCounts = getTopicPartitionMessageCounts(TOPIC_USER_ACTION_LOG);
            long messageCount = partitionMessageCounts.values().stream().mapToLong(Long::longValue).sum();

            log.info("📊 Kafka Topic '{}' 총 메시지 수: {}건", TOPIC_USER_ACTION_LOG, messageCount);

            if (messageCount == 0) {
                log.warn("⚠️ Topic에 메시지가 없습니다. Kafka 전송이 완료되지 않았을 수 있습니다.");
            } else {
                log.info("✅ Topic에 메시지가 정상적으로 쌓였습니다.");

                // 파티션별 통계 출력
                printPartitionStatistics(partitionMessageCounts);
            }
        } catch (Exception e) {
            log.error("Kafka Topic 메시지 확인 중 오류 발생: {}", e.getMessage(), e);
        }

        log.info("==========================================");
    }

    /**
     * Kafka Topic의 메시지 수 확인 및 파티션별 상세 정보 반환
     */
    private Map<Integer, Long> getTopicPartitionMessageCounts(String topicName) {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "loadtest-checker-" + System.currentTimeMillis());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        props.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        Map<Integer, Long> partitionMessageCounts = new HashMap<>();

        try (KafkaConsumer<String, Object> consumer = new KafkaConsumer<>(props)) {
            // Topic의 모든 파티션 정보 가져오기
            List<TopicPartition> partitions = new ArrayList<>();
            try (AdminClient adminClient = AdminClient.create(props)) {
                TopicDescription description = adminClient.describeTopics(Collections.singletonList(topicName))
                        .values().get(topicName).get(5, TimeUnit.SECONDS);

                for (int i = 0; i < description.partitions().size(); i++) {
                    partitions.add(new TopicPartition(topicName, i));
                }
            }

            if (partitions.isEmpty()) {
                log.warn("Topic '{}'의 파티션을 찾을 수 없습니다.", topicName);
                return partitionMessageCounts;
            }

            consumer.assign(partitions);
            consumer.seekToEnd(partitions);

            for (TopicPartition partition : partitions) {
                long endOffset = consumer.position(partition);
                consumer.seekToBeginning(Collections.singletonList(partition));
                long startOffset = consumer.position(partition);
                long partitionMessages = Math.max(0, endOffset - startOffset);
                partitionMessageCounts.put(partition.partition(), partitionMessages);

                // 파티션별 상세 정보를 info 레벨로 로깅
                log.info("📊 파티션 {}: offset {} ~ {} (총 {}건)",
                        partition.partition(), startOffset, endOffset, partitionMessages);
            }

            return partitionMessageCounts;
        } catch (Exception e) {
            log.error("Topic 메시지 수 확인 중 오류: {}", e.getMessage(), e);
            return partitionMessageCounts;
        }
    }

    /**
     * Kafka Topic의 총 메시지 수 확인 (하위 호환성 유지)
     */
    private long getTopicMessageCount(String topicName) {
        Map<Integer, Long> partitionCounts = getTopicPartitionMessageCounts(topicName);
        return partitionCounts.values().stream().mapToLong(Long::longValue).sum();
    }

    /**
     * 파티션별 통계 계산 및 출력
     */
    private void printPartitionStatistics(Map<Integer, Long> partitionMessageCounts) {
        if (partitionMessageCounts.isEmpty()) {
            log.warn("⚠️ 파티션별 메시지 수 데이터가 없습니다.");
            return;
        }

        long totalMessages = partitionMessageCounts.values().stream().mapToLong(Long::longValue).sum();
        int partitionCount = partitionMessageCounts.size();

        if (totalMessages == 0) {
            log.warn("⚠️ 총 메시지 수가 0입니다.");
            return;
        }

        // 최소, 최대, 평균 계산
        long minMessages = partitionMessageCounts.values().stream().mapToLong(Long::longValue).min().orElse(0);
        long maxMessages = partitionMessageCounts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        double avgMessages = (double) totalMessages / partitionCount;

        // 표준편차 계산
        double variance = partitionMessageCounts.values().stream()
                .mapToDouble(count -> Math.pow(count - avgMessages, 2))
                .average()
                .orElse(0.0);
        double standardDeviation = Math.sqrt(variance);

        // 파티션별 분포 비율 계산
        log.info("==========================================");
        log.info("📈 파티션별 메시지 분포 통계");
        log.info("==========================================");
        log.info("총 파티션 수: {}개", partitionCount);
        log.info("총 메시지 수: {}건", totalMessages);
        log.info("파티션당 평균 메시지 수: {:.2f}건", String.format("%.2f", avgMessages));
        log.info("최소 메시지 수: {}건 (파티션: {})", minMessages,
                partitionMessageCounts.entrySet().stream()
                        .filter(e -> e.getValue() == minMessages)
                        .map(e -> String.valueOf(e.getKey()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("N/A"));
        log.info("최대 메시지 수: {}건 (파티션: {})", maxMessages,
                partitionMessageCounts.entrySet().stream()
                        .filter(e -> e.getValue() == maxMessages)
                        .map(e -> String.valueOf(e.getKey()))
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("N/A"));
        log.info("표준편차: {}건", String.format("%.2f", standardDeviation));
        log.info("변동계수 (CV): {}%", String.format("%.2f", (standardDeviation / avgMessages) * 100));

        // 파티션별 분포 비율 출력
        log.info("------------------------------------------");
        log.info("파티션별 메시지 분포 비율:");
        partitionMessageCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    double percentage = (entry.getValue() * 100.0) / totalMessages;
                    log.info("  파티션 {}: {}건 ({}%)", entry.getKey(), entry.getValue(), String.format("%.2f", percentage));
                });
        log.info("==========================================");
    }

    /**
     * Kafka Topic 초기화: Topic 삭제 후 재생성
     * ⚠️ 주의: 프로덕션 환경에서는 절대 사용하지 마세요! (데이터 손실 위험)
     * 테스트/개발 환경에서만 사용하세요.
     */
    private void resetKafkaTopics() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        try (AdminClient adminClient = AdminClient.create(configs)) {
            // 1. 기존 Topic 삭제
            List<String> topicsToDelete = Arrays.asList(TOPIC_USER_ACTION_LOG, TOPIC_DLQ);

            // Topic 존재 여부 확인
            Set<String> existingTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            List<String> topicsToDeleteFiltered = new ArrayList<>();

            for (String topic : topicsToDelete) {
                if (existingTopics.contains(topic)) {
                    topicsToDeleteFiltered.add(topic);
                    log.info("삭제할 Topic 발견: {}", topic);
                } else {
                    log.info("Topic이 존재하지 않음 (건너뜀): {}", topic);
                }
            }

            if (!topicsToDeleteFiltered.isEmpty()) {
                log.info("Topic 삭제 중: {}", topicsToDeleteFiltered);
                DeleteTopicsResult deleteResult = adminClient.deleteTopics(topicsToDeleteFiltered);
                deleteResult.all().get(10, TimeUnit.SECONDS);
                log.info("✅ Topic 삭제 요청 완료: {}", topicsToDeleteFiltered);

                // Topic 삭제가 실제로 완료되었는지 확인 (KRaft 모드에서는 더 오래 걸릴 수 있음)
                log.info("Topic 삭제 완료 확인 중... (KRaft 모드에서는 최대 30초 소요 가능)");
                int maxDeleteWaitRetries = 15; // 최대 15번 시도 (30초)
                boolean topicsDeleted = false;
                for (int retry = 0; retry < maxDeleteWaitRetries; retry++) {
                    Set<String> currentTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                    boolean allDeleted = true;
                    for (String topic : topicsToDeleteFiltered) {
                        if (currentTopics.contains(topic)) {
                            allDeleted = false;
                            break;
                        }
                    }
                    if (allDeleted) {
                        topicsDeleted = true;
                        log.info("✅ Topic 삭제 완료 확인됨 (시도 {}/{})", retry + 1, maxDeleteWaitRetries);
                        break;
                    } else {
                        log.info("Topic 삭제 대기 중... (시도 {}/{})", retry + 1, maxDeleteWaitRetries);
                        Thread.sleep(2000); // 2초 대기
                    }
                }
                
                if (!topicsDeleted) {
                    log.warn("⚠️ Topic 삭제 확인 실패. 일부 Topic이 아직 존재할 수 있습니다.");
                }

                // Topic 삭제 완료 후 클러스터 안정화 대기 (3 Broker Cluster + Consumer 재연결 시간)
                log.info("Topic 삭제 후 클러스터 안정화 및 Consumer 재연결 대기 중... (3 Broker Cluster)");
                Thread.sleep(10000); // 10초 추가 대기 (Consumer 메타데이터 갱신 시간 확보)
            }

            // 2. Topic 재생성 (존재하지 않는 경우에만 생성)
            // user_action_log: 10개 파티션, RF 3
            // dlq_user_action: 1개 파티션, RF 3 (DLQ는 단일 파티션)
            
            // 기존 Topic이 존재하는 경우 파티션 수 확인 및 필요시 삭제
            Set<String> currentTopicsAfterDelete = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
            List<String> topicsToRecreate = new ArrayList<>();
            
            // user_action_log 토픽 파티션 수 확인
            if (currentTopicsAfterDelete.contains(TOPIC_USER_ACTION_LOG)) {
                try {
                    TopicDescription description = adminClient.describeTopics(Collections.singletonList(TOPIC_USER_ACTION_LOG))
                            .values().get(TOPIC_USER_ACTION_LOG).get(5, TimeUnit.SECONDS);
                    int currentPartitions = description.partitions().size();
                    log.info("기존 Topic '{}' 발견: 파티션 수 = {}개 (목표: {}개)", 
                            TOPIC_USER_ACTION_LOG, currentPartitions, TOPIC_PARTITIONS);
                    
                    if (currentPartitions != TOPIC_PARTITIONS) {
                        log.warn("⚠️ Topic '{}'의 파티션 수가 올바르지 않습니다! (현재: {}, 목표: {})", 
                                TOPIC_USER_ACTION_LOG, currentPartitions, TOPIC_PARTITIONS);
                        log.warn("⚠️ Topic을 삭제하고 올바른 파티션 수로 재생성합니다.");
                        topicsToRecreate.add(TOPIC_USER_ACTION_LOG);
                    } else {
                        log.info("✅ Topic '{}'의 파티션 수가 올바릅니다. ({}개)", 
                                TOPIC_USER_ACTION_LOG, currentPartitions);
                    }
                } catch (Exception e) {
                    log.warn("Topic '{}' 파티션 수 확인 실패. 재생성합니다: {}", TOPIC_USER_ACTION_LOG, e.getMessage());
                    topicsToRecreate.add(TOPIC_USER_ACTION_LOG);
                }
            }
            
            // dlq_user_action 토픽 파티션 수 확인
            if (currentTopicsAfterDelete.contains(TOPIC_DLQ)) {
                try {
                    TopicDescription description = adminClient.describeTopics(Collections.singletonList(TOPIC_DLQ))
                            .values().get(TOPIC_DLQ).get(5, TimeUnit.SECONDS);
                    int currentPartitions = description.partitions().size();
                    log.info("기존 Topic '{}' 발견: 파티션 수 = {}개 (목표: 1개)", 
                            TOPIC_DLQ, currentPartitions);
                    
                    if (currentPartitions != 1) {
                        log.warn("⚠️ Topic '{}'의 파티션 수가 올바르지 않습니다! (현재: {}, 목표: 1)", 
                                TOPIC_DLQ, currentPartitions);
                        log.warn("⚠️ Topic을 삭제하고 올바른 파티션 수로 재생성합니다.");
                        topicsToRecreate.add(TOPIC_DLQ);
                    } else {
                        log.info("✅ Topic '{}'의 파티션 수가 올바릅니다. (1개)", TOPIC_DLQ);
                    }
                } catch (Exception e) {
                    log.warn("Topic '{}' 파티션 수 확인 실패. 재생성합니다: {}", TOPIC_DLQ, e.getMessage());
                    topicsToRecreate.add(TOPIC_DLQ);
                }
            }
            
            // 파티션 수가 잘못된 Topic 삭제
            if (!topicsToRecreate.isEmpty()) {
                log.info("파티션 수가 잘못된 Topic 삭제 중: {}", topicsToRecreate);
                DeleteTopicsResult deleteResult = adminClient.deleteTopics(topicsToRecreate);
                deleteResult.all().get(10, TimeUnit.SECONDS);
                log.info("✅ Topic 삭제 요청 완료: {}", topicsToRecreate);
                
                // 삭제 완료 확인
                int maxDeleteWaitRetries = 15;
                boolean allDeleted = false;
                for (int retry = 0; retry < maxDeleteWaitRetries; retry++) {
                    Set<String> currentTopics = adminClient.listTopics().names().get(5, TimeUnit.SECONDS);
                    allDeleted = true;
                    for (String topic : topicsToRecreate) {
                        if (currentTopics.contains(topic)) {
                            allDeleted = false;
                            break;
                        }
                    }
                    if (allDeleted) {
                        log.info("✅ Topic 삭제 완료 확인됨 (시도 {}/{})", retry + 1, maxDeleteWaitRetries);
                        break;
                    } else {
                        log.info("Topic 삭제 대기 중... (시도 {}/{})", retry + 1, maxDeleteWaitRetries);
                        Thread.sleep(2000);
                    }
                }
                
                if (!allDeleted) {
                    log.warn("⚠️ 일부 Topic 삭제 확인 실패. 재생성을 시도합니다.");
                }
                
                Thread.sleep(5000); // 삭제 후 안정화 대기
            }
            
            List<NewTopic> newTopics = Arrays.asList(
                    new NewTopic(TOPIC_USER_ACTION_LOG, TOPIC_PARTITIONS, TOPIC_REPLICATION_FACTOR),
                    new NewTopic(TOPIC_DLQ, 1, TOPIC_REPLICATION_FACTOR)  // DLQ는 1개 파티션
            );

            log.info("Topic 생성 중: {} (파티션: {}개, {}개)", 
                    Arrays.asList(TOPIC_USER_ACTION_LOG, TOPIC_DLQ), TOPIC_PARTITIONS, 1);

            // Topic 생성 시도
            try {
                CreateTopicsResult createResult = adminClient.createTopics(newTopics);
                createResult.all().get(10, TimeUnit.SECONDS);
                log.info("✅ Topic 생성 완료");
            } catch (ExecutionException e) {
                if (e.getCause() instanceof org.apache.kafka.common.errors.TopicExistsException) {
                    // 파티션 수 확인 후 재시도했는데도 존재한다면, 파티션 수가 올바른 상태일 가능성이 높음
                    // 하지만 경고를 남기고 확인
                    log.warn("⚠️ Topic이 이미 존재합니다. 파티션 수를 다시 확인합니다.");
                    // 파티션 수 재확인 로직은 위에서 이미 처리했으므로, 여기서는 로그만 남김
                } else {
                    // 다른 에러는 다시 throw
                    throw e;
                }
            }

            // 3. Topic 생성 확인 및 메타데이터 업데이트 대기
            log.info("Topic 생성 확인 및 메타데이터 업데이트 대기 중...");
            Thread.sleep(3000); // 3초 대기 (메타데이터 업데이트 시간)

            // Topic 존재 여부 재확인 (최대 3번 시도)
            for (int retry = 0; retry < 3; retry++) {
                boolean allTopicsExist = true;
                for (String topic : Arrays.asList(TOPIC_USER_ACTION_LOG, TOPIC_DLQ)) {
                    try {
                        TopicDescription description = adminClient.describeTopics(Collections.singletonList(topic))
                                .values().get(topic).get(5, TimeUnit.SECONDS);
                        log.info("✅ Topic 확인 완료: {} (파티션: {})", topic, description.partitions().size());
                    } catch (Exception e) {
                        log.warn("Topic 확인 실패 (시도 {}/3): {} - {}", retry + 1, topic, e.getMessage());
                        allTopicsExist = false;
                    }
                }

                if (allTopicsExist) {
                    log.info("✅ 모든 Topic이 정상적으로 생성되었습니다.");
                    break;
                } else if (retry < 2) {
                    log.info("Topic 재확인 대기 중... ({}초)", (retry + 1) * 2);
                    Thread.sleep((retry + 1) * 2000); // 2초, 4초, 6초 대기
                } else {
                    log.warn("⚠️ 일부 Topic 확인 실패. 테스트는 계속 진행됩니다.");
                }
            }

            // 메타데이터 완전히 업데이트될 때까지 추가 대기 (3 Broker Cluster + Consumer 재연결)
            log.info("Kafka 메타데이터 완전 업데이트 및 Consumer 재연결 대기 중... (3 Broker Cluster)");
            Thread.sleep(10000); // 10초 추가 대기 (3 Broker Cluster 안정화 + Consumer 메타데이터 갱신)

            // 모든 브로커에 메타데이터가 전파되었는지 확인
            log.info("브로커 간 메타데이터 동기화 및 Consumer 재연결 확인 중...");
            Thread.sleep(10000); // 10초 추가 대기 (Consumer가 새 Topic을 인식할 시간 확보)

        } catch (Exception e) {
            log.error("Kafka Topic 초기화 중 오류 발생: {}", e.getMessage(), e);
            throw new RuntimeException("Kafka Topic 초기화 실패", e);
        }
    }

}

