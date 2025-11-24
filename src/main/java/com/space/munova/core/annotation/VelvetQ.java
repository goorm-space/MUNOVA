package com.space.munova.core.annotation;

import com.space.munova.velvetQ.dto.VelvetQDomainType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface VelvetQ {

    // 대기열 큐 적용 도메인
    VelvetQDomainType domain();

    // key값으로 사용할 고유 ID
    String resourceId();
}
