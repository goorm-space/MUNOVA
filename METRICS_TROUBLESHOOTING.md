# 🔍 Redis Stream Consumer Latency 메트릭 확인 가이드

## 1️⃣ Prometheus에서 메트릭 확인 방법

### Step 1: Prometheus Targets 확인

1. **Prometheus UI 접속**: `http://localhost:9090`
2. **Status → Targets** 메뉴 클릭
3. 다음 항목들이 **UP** 상태인지 확인:
   - `recommend-server` (1개)
   - `recommend-consumers` (10개 - 모두 UP이어야 함)

**문제가 있는 경우:**
- **DOWN** 상태라면 → 추천 서버가 실행 중인지 확인
- **Connection refused** → 포트가 열려있는지 확인
- **Timeout** → 네트워크 연결 확인

### Step 2: 메트릭이 실제로 수집되는지 확인

**Prometheus UI에서 직접 쿼리:**

1. **Graph** 탭으로 이동
2. 검색창에 다음 쿼리 입력:

```promql
# 1. 모든 메트릭 목록 확인 (redis_stream으로 시작하는 것들)
{__name__=~"redis_stream.*"}

# 2. Latency 히스토그램 확인
redis_stream_consumer_latency_ms

# 3. 메시지 카운터 확인
redis_stream_consumer_messages_total

# 4. Consumer별 평균 latency 확인
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])

# 5. Consumer별 95th percentile latency
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))
```

**메트릭이 보이지 않는 경우:**
- 추천 서버에서 실제로 메시지를 처리하고 있는지 확인
- 추천 서버의 `/metrics` 엔드포인트에서 직접 확인:
  ```bash
  curl http://localhost:8001/metrics | grep redis_stream
  curl http://localhost:8002/metrics | grep redis_stream
  ```

### Step 3: 메트릭 엔드포인트 직접 확인

```bash
# Recommend Server 메트릭 확인
curl http://localhost:8001/metrics | grep redis_stream

# Consumer 메트릭 확인 (예시)
curl http://localhost:8002/metrics | grep redis_stream
```

**예상 출력:**
```
# HELP redis_stream_consumer_latency_ms Redis Stream Consumer Latency (milliseconds)
# TYPE redis_stream_consumer_latency_ms histogram
redis_stream_consumer_latency_ms_bucket{stream_key="product_action_stream",event_type="product_detail_view",le="10"} 5
redis_stream_consumer_latency_ms_bucket{stream_key="product_action_stream",event_type="product_detail_view",le="50"} 12
...
redis_stream_consumer_latency_ms_sum{stream_key="product_action_stream",event_type="product_detail_view"} 1234
redis_stream_consumer_latency_ms_count{stream_key="product_action_stream",event_type="product_detail_view"} 50

# HELP redis_stream_consumer_messages_total Total number of messages consumed from Redis Stream
# TYPE redis_stream_consumer_messages_total counter
redis_stream_consumer_messages_total{stream_key="product_action_stream",has_producer_time="true"} 100
```

---

## 2️⃣ Grafana에서 메트릭 확인 방법

### 방법 A: Explore에서 직접 쿼리 (빠른 확인)

1. **Grafana 접속**: `http://localhost:3000`
   - ID: `admin` / Password: `admin`
2. **Explore** 메뉴 클릭 (왼쪽 사이드바)
3. **데이터소스 선택**: `Prometheus` 선택
4. **쿼리 입력**:

```promql
# 평균 Latency (ms)
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])

# 95th Percentile Latency (ms)
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))

# 처리된 메시지 수 (초당)
rate(redis_stream_consumer_messages_total[5m])

# Consumer별 Latency 분포
histogram_quantile(0.50, rate(redis_stream_consumer_latency_ms_bucket[5m]))  # 중앙값
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))  # 95th percentile
histogram_quantile(0.99, rate(redis_stream_consumer_latency_ms_bucket[5m]))  # 99th percentile
```

5. **Run query** 클릭하여 그래프 확인

### 방법 B: 대시보드에 패널 추가 (영구적 모니터링)

#### Step 1: 기존 대시보드 열기

1. **Dashboards** → **Browse** 메뉴
2. `Redis Stream Dashboard` 또는 원하는 대시보드 선택
3. **Edit** 버튼 클릭 (상단 오른쪽)

#### Step 2: 새 패널 추가

1. **Add** → **Visualization** 클릭
2. **Query** 탭에서:
   - **Data source**: `Prometheus` 선택
   - **Query A** 입력:

```promql
# 패널 1: 평균 Latency
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```

3. **Panel options**:
   - **Title**: `Redis Stream Consumer - Average Latency`
   - **Unit**: `ms (milliseconds)`
   - **Legend**: `{{stream_key}} - {{event_type}}`

4. **Apply** 클릭

#### Step 3: 추가 패널들

**패널 2: Latency 분포 (히스토그램)**
```promql
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))
```
- **Title**: `95th Percentile Latency`
- **Unit**: `ms`

**패널 3: 메시지 처리량**
```promql
rate(redis_stream_consumer_messages_total[5m])
```
- **Title**: `Messages Processed per Second`
- **Unit**: `ops/sec`

**패널 4: Consumer별 Latency 비교 (테이블)**
```promql
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```
- **Visualization**: `Table`
- **Title**: `Consumer Latency by Stream`

#### Step 4: 대시보드 저장

1. 상단 **Save** 버튼 클릭
2. 변경사항 저장

---

## 3️⃣ 문제 해결 체크리스트

### ✅ 메트릭이 보이지 않는 경우

- [ ] **추천 서버가 실행 중인가?**
  ```bash
  docker ps | grep recommend
  ```

- [ ] **메트릭 엔드포인트가 응답하는가?**
  ```bash
  curl http://localhost:8001/metrics
  curl http://localhost:8002/metrics
  ```

- [ ] **Prometheus가 추천 서버에 연결할 수 있는가?**
  - Prometheus UI → Status → Targets 확인
  - `recommend-server`, `recommend-consumers` 모두 UP 상태인지 확인

- [ ] **추천 서버에서 실제로 메시지를 처리하고 있는가?**
  - Consumer 로그 확인
  - Redis Stream에 메시지가 있는지 확인:
    ```bash
    redis-cli XINFO STREAM product_action_stream
    ```

- [ ] **메트릭 이름이 정확한가?**
  - 추천 서버 코드에서 정의한 메트릭 이름 확인
  - 예: `redis_stream_consumer_latency_ms` (정확히 일치해야 함)

- [ ] **Prometheus 설정이 올바른가?**
  - `prometheus.yml` 파일 확인
  - `recommend-server`, `recommend-consumers` job이 있는지 확인

- [ ] **Prometheus를 재시작했는가?**
  ```bash
  docker-compose restart prometheus
  ```

### ✅ 메트릭은 보이지만 값이 0인 경우

- [ ] **실제로 메시지가 처리되고 있는가?**
  - API 서버에서 로그를 보내고 있는지 확인
  - Consumer가 메시지를 읽고 있는지 확인

- [ ] **시간 범위가 올바른가?**
  - Prometheus/Grafana에서 최근 5분~1시간 데이터 확인

---

## 4️⃣ 빠른 확인 명령어 모음

```bash
# 1. 추천 서버 컨테이너 상태 확인
docker ps | grep recommend

# 2. 메트릭 엔드포인트 확인
curl -s http://localhost:8001/metrics | grep redis_stream | head -20
curl -s http://localhost:8002/metrics | grep redis_stream | head -20

# 3. Prometheus Targets 확인 (브라우저)
# http://localhost:9090/targets

# 4. Prometheus에서 메트릭 쿼리 (브라우저)
# http://localhost:9090/graph?g0.expr=redis_stream_consumer_latency_ms

# 5. Redis Stream에 메시지가 있는지 확인
docker exec -it redis-node-1 redis-cli -a 1234 XINFO STREAM product_action_stream

# 6. Prometheus 로그 확인
docker logs prometheus --tail 50
```

---

## 5️⃣ 예상되는 정상 동작

### 정상적인 경우:

1. **Prometheus Targets**: 모두 UP (녹색)
2. **메트릭 쿼리**: 값이 표시됨 (0이 아닌 값)
3. **Grafana Explore**: 그래프가 그려짐
4. **메트릭 이름**: `redis_stream_consumer_latency_ms*` 형태로 여러 메트릭이 보임

### 메트릭이 나타나는 시점:

- 추천 서버가 시작되고 메시지를 **처리하기 시작한 후**부터 메트릭이 나타납니다.
- 메시지가 처리되지 않으면 메트릭이 0이거나 나타나지 않을 수 있습니다.

