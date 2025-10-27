package com.space.munova.core.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class TimeHelper {

    /**
     * 만료시간까지 남은 시간 초(Long)단위로 반환
     */
    public static Long remainTimeToSeconds(LocalDateTime expireTime) {
        Duration duration = Duration.between(LocalDateTime.now(), expireTime);
        long ttlSeconds = duration.getSeconds();
        return ttlSeconds <= 0 ? 0L : ttlSeconds;
    }

    /**
     * targetTime을 ms로 변경
     */
    public static Long toEpochMilli(LocalDateTime targetTime) {
        return targetTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

}
