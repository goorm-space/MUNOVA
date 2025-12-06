# Grafana 대시보드 임포트 가이드

## 🎯 공식 Grafana 대시보드 (Grafana.com에서 임포트)

### 1. Node Exporter (시스템 메트릭)
- **대시보드 ID**: `1860`
- **제목**: Node Exporter Full
- **임포트 URL**: https://grafana.com/grafana/dashboards/1860

### 2. Spring Boot (JVM 메트릭)
- **대시보드 ID**: `11378`
- **제목**: Spring Boot 2.1 Statistics
- **임포트 URL**: https://grafana.com/grafana/dashboards/11378

### 3. MySQL Exporter
- **대시보드 ID**: `7362`
- **제목**: MySQL Overview
- **임포트 URL**: https://grafana.com/grafana/dashboards/7362

### 4. MongoDB Exporter
- **대시보드 ID**: `2583`
- **제목**: MongoDB Exporter
- **임포트 URL**: https://grafana.com/grafana/dashboards/2583

### 5. Redis Exporter
- **대시보드 ID**: `11835`
- **제목**: Redis Dashboard for Prometheus Redis Exporter
- **임포트 URL**: https://grafana.com/grafana/dashboards/11835

### 6. Elasticsearch
- **대시보드 ID**: `2322`
- **제목**: Elasticsearch
- **임포트 URL**: https://grafana.com/grafana/dashboards/2322

---

## 📝 Grafana에서 임포트하는 방법

### 방법 1: Dashboard ID로 임포트 (가장 간단)

1. Grafana 접속: `http://172.16.24.237:3001`
2. 좌측 메뉴: **Dashboards** → **Import**
3. **Import via grafana.com** 섹션에 대시보드 ID 입력 (예: `1860`)
4. **Load** 클릭
5. Prometheus 데이터소스 선택
6. **Import** 클릭

### 방법 2: JSON 파일로 임포트

1. Grafana 접속
2. 좌측 메뉴: **Dashboards** → **Import**
3. **Upload JSON file** 클릭
4. JSON 파일 선택
5. **Import** 클릭

---

## 🎨 커스텀 대시보드 (MUNOVA 전용)

아래는 MUNOVA 애플리케이션 전용 대시보드 쿼리들입니다. 직접 패널을 만들어서 사용하세요.

### Spring Boot 애플리케이션 메트릭

#### 1. HTTP 요청 수 (QPS)
```promql
sum(rate(http_server_requests_seconds_count{application="MUNOVA"}[5m])) by (uri, method)
```

#### 2. 평균 응답 시간
```promql
rate(http_server_requests_seconds_sum{application="MUNOVA", uri="/product"}[5m]) / 
rate(http_server_requests_seconds_count{application="MUNOVA", uri="/product"}[5m])
```

#### 3. 95th percentile 응답 시간
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket{application="MUNOVA", uri="/product"}[5m])) by (le, uri)
)
```

#### 4. 에러율 (5xx)
```promql
sum(rate(http_server_requests_seconds_count{application="MUNOVA", status=~"5.."}[5m])) / 
sum(rate(http_server_requests_seconds_count{application="MUNOVA"}[5m])) * 100
```

#### 5. JVM 메모리 사용률
```promql
sum(jvm_memory_used_bytes{application="MUNOVA", area="heap"}) / 
sum(jvm_memory_max_bytes{application="MUNOVA", area="heap"}) * 100
```

#### 6. JVM 힙 메모리 사용량
```promql
jvm_memory_used_bytes{application="MUNOVA", area="heap", id="G1 Survivor Space"}
```

#### 7. JVM GC 시간
```promql
rate(jvm_gc_pause_seconds_sum{application="MUNOVA"}[5m])
```

#### 8. CPU 사용률
```promql
process_cpu_usage{application="MUNOVA"} * 100
```

#### 9. HikariCP 활성 연결 수
```promql
hikari_connections_active{application="MUNOVA"}
```

#### 10. HikariCP 대기 연결 수
```promql
hikari_connections_idle{application="MUNOVA"}
```

---

### 시스템 메트릭 (Node Exporter)

#### 1. CPU 사용률
```promql
100 - (avg by (instance) (rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

#### 2. 메모리 사용률
```promql
(1 - (node_memory_MemAvailable_bytes / node_memory_MemTotal_bytes)) * 100
```

#### 3. 디스크 사용률
```promql
100 - ((node_filesystem_avail_bytes{mountpoint="/"} * 100) / node_filesystem_size_bytes{mountpoint="/"})
```

#### 4. 네트워크 입출력
```promql
rate(node_network_receive_bytes_total[5m])
rate(node_network_transmit_bytes_total[5m])
```

---

### MySQL 메트릭

#### 1. 연결 수
```promql
mysql_global_status_threads_connected{instance="munova-mysql"}
```

#### 2. 쿼리 수
```promql
rate(mysql_global_status_queries{instance="munova-mysql"}[5m])
```

#### 3. 슬로우 쿼리
```promql
rate(mysql_global_status_slow_queries{instance="munova-mysql"}[5m])
```

---

### MongoDB 메트릭

#### 1. 연결 수
```promql
mongodb_connections{instance="munova-mongodb"}
```

#### 2. 쿼리 수
```promql
rate(mongodb_opcounters{instance="munova-mongodb"}[5m])
```

#### 3. 메모리 사용량
```promql
mongodb_memory{instance="munova-mongodb"}
```

---

### Redis 메트릭

#### 1. 연결 수
```promql
redis_connected_clients{instance="munova-redis"}
```

#### 2. 명령 실행 수
```promql
rate(redis_commands_total{instance="munova-redis"}[5m])
```

#### 3. 메모리 사용량
```promql
redis_memory_used_bytes{instance="munova-redis"}
```

#### 4. 키 개수
```promql
redis_keyspace_keys{instance="munova-redis"}
```

---

### Elasticsearch 메트릭

#### 1. 클러스터 상태
```promql
elasticsearch_cluster_health_status{instance="munova-elasticsearch"}
```

#### 2. 검색 쿼리 수
```promql
rate(elasticsearch_indices_search_query_total{instance="munova-elasticsearch"}[5m])
```

#### 3. 인덱싱 속도
```promql
rate(elasticsearch_indices_indexing_index_total{instance="munova-elasticsearch"}[5m])
```

#### 4. JVM 힙 메모리
```promql
elasticsearch_jvm_memory_used_bytes{instance="munova-elasticsearch", area="heap"}
```

---

## 🚀 빠른 시작 가이드

### 1단계: 공식 대시보드 임포트

1. **Node Exporter Full** (ID: 1860)
2. **Spring Boot Statistics** (ID: 11378)
3. **MySQL Overview** (ID: 7362)
4. **Redis Dashboard** (ID: 11835)

### 2단계: 커스텀 패널 추가

각 공식 대시보드에 MUNOVA 전용 패널을 추가하세요.

### 3단계: 통합 대시보드 생성

모든 메트릭을 한 화면에 보는 통합 대시보드를 만들 수도 있습니다.

---

## 📌 참고사항

- 모든 쿼리는 Prometheus 데이터소스를 사용합니다
- 데이터소스 이름이 `Prometheus`가 아닌 경우 쿼리를 수정해야 합니다
- 일부 메트릭 이름은 실제 환경에 따라 다를 수 있습니다
- Prometheus에서 메트릭을 먼저 확인하세요: http://172.16.24.237:9090

---

## 🔍 메트릭 확인 방법

### Prometheus에서 메트릭 확인
```bash
# 사용 가능한 메트릭 확인
curl http://172.16.24.237:9090/api/v1/label/__name__/values | jq

# 특정 메트릭 확인
curl "http://172.16.24.237:9090/api/v1/query?query=http_server_requests_seconds_count" | jq
```

