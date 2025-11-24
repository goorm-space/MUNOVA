package com.space.munova.core.aop;

import com.space.munova.core.annotation.RedisDistributeLock;
import com.space.munova.core.utils.AopParseKey;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

import static com.space.munova.core.config.StaticVariables.REDISSON_LOCK_PREFIX;

/**
 * @RedisDistributeLock 수행시 실행
 * 트랜잭션 AOP보다 먼저 실행되도록 @Order 적용
 * - 대상 메서드(proceed()) 종료 후 락 해제
 * - 에러가 나더라도 락 해제
 */
@Aspect
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RedisDistributedLockAop {

    private final RedissonClient redissonClient;

    @Around("@annotation(com.space.munova.core.annotation.RedisDistributeLock)")
    public Object distributeLock(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RedisDistributeLock annotation = method.getAnnotation(RedisDistributeLock.class);

        String key = REDISSON_LOCK_PREFIX + AopParseKey.parseKey(
                signature.getParameterNames(),
                joinPoint.getArgs(),
                annotation.key()
        );
        RLock rLock = redissonClient.getLock(key);

        try {
            boolean isLocked = rLock.tryLock(
                    annotation.waitTime(),
                    annotation.leaseTime(),
                    annotation.timeUnit()
            );
            if (!isLocked) {
                throw new IllegalStateException("락을 획득하지 못했습니다.");
            }
            return joinPoint.proceed();
        } finally {
            if (rLock.isHeldByCurrentThread()) {
                rLock.unlock();
            }
        }

    }

}
