package com.space.munova.common;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

// 스레드풀 기반 동시 작업
// - 범용 동시성 테스트 헬퍼 클래스
@Slf4j
@Getter
public class ConcurrencyTestHelper {

    private final int threadCount;
    private final ExecutorService executorService;
    private final CountDownLatch startLatch;
    private final CountDownLatch endLatch;
    private final AtomicInteger successCount;
    private final AtomicInteger failureCount;
    private final List<Exception> exceptions;

    public ConcurrencyTestHelper(int threadCount) {
        this.threadCount = threadCount;
        this.executorService = Executors.newFixedThreadPool(threadCount);
        this.startLatch = new CountDownLatch(1);
        this.endLatch = new CountDownLatch(threadCount);
        this.successCount = new AtomicInteger(0);
        this.failureCount = new AtomicInteger(0);
        this.exceptions = new ArrayList<>();
    }

    // 동시성 작업 실행
    public void execute(ConcurrentTask task) throws InterruptedException {
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    task.run(index);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    exceptions.add(e);
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 스레드 시작
        startLatch.countDown();
        // 스레드 종료 대기
        endLatch.await();
        // 종료
        executorService.shutdown();
    }

    // 동시성 작업 실행
    // ExceptionPredicate - 특정 예외를 실패로 분류
    public void execute(ConcurrentTask task, ExceptionPredicate exceptionPredicate) throws InterruptedException {
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executorService.execute(() -> {
                try {
                    startLatch.await();
                    task.run(index);
                    successCount.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    if (exceptionPredicate.test(e)) {
                        failureCount.incrementAndGet();
                    } else {
                        exceptions.add(e);
                    }
                } finally {
                    endLatch.countDown();
                }
            });
        }

        // 스레드 시작
        startLatch.countDown();
        // 스레드 종료 대기
        endLatch.await();
        // 종료
        executorService.shutdown();
    }

    public int getSuccessCount() {
        return successCount.get();
    }

    public int getFailureCount() {
        return failureCount.get();
    }

    // 동시성 작업
    @FunctionalInterface
    public interface ConcurrentTask {
        void run(int index) throws Exception;
    }

    // 예외 판정
    // - 발생한 예외가 예측된 예외인지, 실패로 분류할 예외인지 판정
    @FunctionalInterface
    public interface ExceptionPredicate {
        // 예측된 예외인 경우 true, 그 외 false
        boolean test(Exception exception);
    }
}
