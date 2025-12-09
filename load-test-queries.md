# 부하 테스트 모니터링 쿼리 (중요도 순)

## 🔴 최우선 (Critical) - 시스템 안정성

### 1. 애플리케이션 상태
```promql
# 애플리케이션 가용성
up{job="munova-api"}

# HTTP 요청 에러율
rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) * 100

# HTTP 요청 처리량
rate(http_server_requests_seconds_count[5m])
```

### 2. 응답 시간 (Latency)
```promql
# 평균 응답 시간
rate(http_server_requests_seconds_sum[5m]) / rate(http_server_requests_seconds_count[5m])

# P95 응답 시간
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))

# P99 응답 시간
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

### 3. 시스템 리소스 (CPU, Memory)
```promql
# CPU 사용률
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)

# 메모리 사용률
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100

# 디스크 사용률
(1 - (node_filesystem_avail_bytes / node_filesystem_size_bytes)) * 100
```

---

## 🟠 높음 (High) - 성능 병목 지점

### 4. 데이터베이스 연결 및 성능
```promql
# MySQL 연결 수
mysql_global_status_threads_connected

# MySQL 활성 연결 수
mysql_global_status_threads_running

# MongoDB 활성 연결 수
mongodb_ss_connections{conn_type="active"}

# MongoDB 사용 가능한 연결 수
mongodb_ss_connections{conn_type="available"}
```

### 5. 데이터베이스 쿼리 성능
```promql
# MySQL 느린 쿼리 수
rate(mysql_global_status_slow_queries[5m])

# MySQL 쿼리 처리량
rate(mysql_global_status_questions[5m])

# MongoDB 작업 카운터 (초당)
rate(mongodb_ss_opcounters{op_type="query"}[5m])
rate(mongodb_ss_opcounters{op_type="insert"}[5m])
rate(mongodb_ss_opcounters{op_type="update"}[5m])
rate(mongodb_ss_opcounters{op_type="delete"}[5m])
```

### 6. Redis 성능
```promql
# Redis 연결 수
redis_connected_clients

# Redis 메모리 사용량
redis_memory_used_bytes

# Redis 명령 처리량
rate(redis_commands_total[5m])

# Redis 히트율 (캐시 효율)
(1 - (rate(redis_keyspace_misses_total[5m]) / (rate(redis_keyspace_hits_total[5m]) + rate(redis_keyspace_misses_total[5m])))) * 100
```

### 7. Elasticsearch 성능
```promql
# Elasticsearch 클러스터 상태
elasticsearch_cluster_health_status

# Elasticsearch 검색 쿼리 처리량
rate(elasticsearch_indices_search_query_time_seconds_count[5m])

# Elasticsearch 인덱싱 처리량
rate(elasticsearch_indices_indexing_index_time_seconds_count[5m])

# Elasticsearch JVM 힙 메모리 사용률
(elasticsearch_jvm_memory_used_bytes{area="heap"} / elasticsearch_jvm_memory_max_bytes{area="heap"}) * 100
```

---

## 🟡 중간 (Medium) - 세부 모니터링

### 8. 네트워크 트래픽
```promql
# 네트워크 수신 트래픽
rate(node_network_receive_bytes_total[5m])

# 네트워크 송신 트래픽
rate(node_network_transmit_bytes_total[5m])

# MongoDB 네트워크 트래픽
rate(mongodb_ss_network_bytesIn[5m])
rate(mongodb_ss_network_bytesOut[5m])
```

### 9. 데이터베이스 크기 및 성장률
```promql
# MongoDB 데이터베이스 크기
mongodb_dbstats_dataSize{exported_database="munova_db"}

# MongoDB 인덱스 크기
mongodb_dbstats_indexSize{exported_database="munova_db"}

# MySQL 데이터베이스 크기
mysql_info_schema_size_bytes
```

### 10. JVM 메모리 (Spring Boot)
```promql
# JVM 힙 메모리 사용량
jvm_memory_used_bytes{area="heap"}

# JVM 힙 메모리 사용률
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# GC 시간
rate(jvm_gc_pause_seconds_sum[5m])

# GC 빈도
rate(jvm_gc_pause_seconds_count[5m])
```

### 11. 커넥션 풀 (HikariCP)
```promql
# 활성 커넥션 수
hikaricp_connections_active

# 대기 중인 커넥션 수
hikaricp_connections_idle

# 커넥션 획득 시간
hikaricp_connections_acquire_seconds
```

---

## 🟢 낮음 (Low) - 참고용

### 12. 세부 메트릭
```promql
# MongoDB 복제 지연 (복제 환경)
mongodb_ss_repl_lag

# Elasticsearch 샤드 상태
elasticsearch_cluster_health_active_shards

# Redis 키 수
redis_db_keys

# MySQL 테이블 잠금
mysql_global_status_table_locks_waited
```

---

## 📊 Grafana 대시보드 권장 패널

### 필수 패널:
1. **시스템 리소스**: CPU, Memory, Disk 사용률
2. **응답 시간**: P50, P95, P99
3. **처리량**: 초당 요청 수 (RPS)
4. **에러율**: 5xx 에러 비율
5. **데이터베이스 연결 수**: MySQL, MongoDB, Redis
6. **데이터베이스 쿼리 처리량**: 초당 쿼리 수

### 권장 패널:
7. **캐시 히트율**: Redis
8. **검색 성능**: Elasticsearch 쿼리 시간
9. **JVM 메모리**: 힙 사용률, GC 시간
10. **네트워크 트래픽**: 수신/송신 바이트

---

## 🎯 부하 테스트 체크리스트

### 테스트 전:
- [ ] 모든 exporter가 정상 동작하는지 확인
- [ ] Prometheus가 모든 타겟을 스크랩하는지 확인
- [ ] Grafana 대시보드가 제대로 설정되어 있는지 확인

### 테스트 중 모니터링:
- [ ] CPU 사용률이 80% 이하인지 확인
- [ ] 메모리 사용률이 90% 이하인지 확인
- [ ] 응답 시간이 SLA 기준 내인지 확인 (예: P95 < 2초)
- [ ] 에러율이 1% 이하인지 확인
- [ ] 데이터베이스 연결 수가 최대치를 넘지 않는지 확인

### 테스트 후 분석:
- [ ] 병목 지점 식별 (CPU, Memory, DB, Network)
- [ ] 느린 쿼리 분석
- [ ] 에러 로그 분석
- [ ] 리소스 사용 패턴 분석

