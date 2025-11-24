package com.space.munova.core.aop;

import com.space.munova.core.annotation.VelvetQ;
import com.space.munova.core.utils.AopParseKey;
import com.space.munova.velvetQ.config.ExternalQueueApi;
import com.space.munova.velvetQ.dto.CheckQueueRequest;
import com.space.munova.velvetQ.dto.ExternalQueueResponse;
import com.space.munova.velvetQ.dto.VelvetQDomainType;
import com.space.munova.velvetQ.exception.VelvetQException;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * @VelvetQ 수행시 실행
 * 대기열큐가 필요한 도메인에 사용
 * - 대기열이 필요할 경우 response로 알림
 * - 대기열이 필요하지 않을 경우 요청한 메서드 실행
 */
@Aspect
@Component
@RequiredArgsConstructor
public class VelvetQAop {

    private final ExternalQueueApi externalQueueApi;

    @Around("@annotation(com.space.munova.core.annotation.VelvetQ)")
    public Object velvetQ(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        VelvetQ annotation = method.getAnnotation(VelvetQ.class);

        VelvetQDomainType domain = annotation.domain();
        String resourceId = AopParseKey.parseKey(
                signature.getParameterNames(),
                joinPoint.getArgs(),
                annotation.resourceId()
        ).toString();

        CheckQueueRequest checkQueueRequest = CheckQueueRequest.of(domain, resourceId);
        // 대기열 필요 여부 확인
        ExternalQueueResponse externalResponse = externalQueueApi.callCheckQueueRequired(checkQueueRequest);
        if (externalResponse.required()) {
            // 대기열 필요
            // - redirect, baseException에서 처리
            throw VelvetQException.redirectWaitingQueueException(externalResponse.redirectUrl());
        } else {
            // 대기열 불필요
            // 요청 이어서 실행
            return joinPoint.proceed();
        }
    }

}
