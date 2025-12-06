# Grafana 대시보드 빠른 임포트 가이드

## 🎯 공식 대시보드 ID 목록

### 필수 대시보드 (순서대로 임포트)

1. **Node Exporter Full** → **ID: `1860`**
   - 시스템 CPU, 메모리, 디스크, 네트워크

2. **Spring Boot 2.1 Statistics** → **ID: `11378`**
   - JVM 메모리, GC, HTTP 요청

3. **MySQL Overview** → **ID: `7362`**
   - MySQL 연결, 쿼리, 성능

4. **Redis Dashboard** → **ID: `11835`**
   - Redis 연결, 메모리, 명령

5. **MongoDB Exporter** → **ID: `2583`**
   - MongoDB 연결, 쿼리, 메모리

6. **Elasticsearch** → **ID: `2322`**
   - Elasticsearch 클러스터, 인덱스, 쿼리

---

## 📝 임포트 방법 (1분)

1. Grafana 접속: `http://172.16.24.237:3001` (ID: admin, PW: admin)

2. 좌측 메뉴: **Dashboards** → **Import**

3. **Import via grafana.com**에 대시보드 ID 입력:
   ```
   1860  → Import
   11378 → Import
   7362  → Import
   11835 → Import
   2583  → Import
   2322  → Import
   ```

4. 각 대시보드에서:
   - Prometheus 데이터소스 선택
   - **Import** 클릭

---

## 🔥 주요 메트릭 쿼리 (커스텀 패널용)

### HTTP 응답 시간 (평균)
```promql
rate(http_server_requests_seconds_sum{application="MUNOVA", uri="/product"}[5m]) / 
rate(http_server_requests_seconds_count{application="MUNOVA", uri="/product"}[5m])
```

### HTTP 요청 수 (초당)
```promql
sum(rate(http_server_requests_seconds_count{application="MUNOVA", uri="/product"}[5m]))
```

### 에러율 (%)
```promql
sum(rate(http_server_requests_seconds_count{application="MUNOVA", status=~"5.."}[5m])) / 
sum(rate(http_server_requests_seconds_count{application="MUNOVA"}[5m])) * 100
```

### JVM 메모리 사용률 (%)
```promql
sum(jvm_memory_used_bytes{application="MUNOVA", area="heap"}) / 
sum(jvm_memory_max_bytes{application="MUNOVA", area="heap"}) * 100
```

### CPU 사용률 (%)
```promql
100 - (avg(rate(node_cpu_seconds_total{mode="idle"}[5m])) * 100)
```

### DB 연결 수
```promql
# MySQL
mysql_global_status_threads_connected{instance="munova-mysql"}

# MongoDB
mongodb_connections{instance="munova-mongodb"}

# Redis
redis_connected_clients{instance="munova-redis"}
```

---

## ✅ 확인 체크리스트

- [ ] Node Exporter 대시보드 (1860) 임포트
- [ ] Spring Boot 대시보드 (11378) 임포트
- [ ] MySQL 대시보드 (7362) 임포트
- [ ] Redis 대시보드 (11835) 임포트
- [ ] MongoDB 대시보드 (2583) 임포트
- [ ] Elasticsearch 대시보드 (2322) 임포트
- [ ] 모든 대시보드에서 데이터 표시 확인

---

## 🚨 문제 해결

### 데이터가 안 보여요
1. Prometheus에서 메트릭 확인: http://172.16.24.237:9090
2. Status → Targets에서 모든 exporter가 UP인지 확인
3. 데이터소스 이름이 `Prometheus`인지 확인

### 대시보드 ID를 찾을 수 없어요
- Grafana.com에서 검색: https://grafana.com/grafana/dashboards/
- 키워드: "node exporter", "spring boot", "mysql", "redis", "mongodb", "elasticsearch"

---

## 📚 상세 가이드

더 자세한 내용은 `grafana-dashboards-guide.md` 파일을 참고하세요.

