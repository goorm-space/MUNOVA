package com.space.munova.log.infra.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserActionLogDto {
    private String eventType;
    private String service;
    private Long memberId;
    private Map<String, Object> data;
    private Instant eventTime;
    private Long eventTimestamp;
    private Long producerTime;
    private Integer version;
}

