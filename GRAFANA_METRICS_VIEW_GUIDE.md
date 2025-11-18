# 📊 Grafana에서 Redis Stream Consumer Latency 메트릭 보는 방법

## 🎯 목표
Grafana에서 `redis_stream_consumer_latency_ms` 메트릭을 확인하는 방법을 단계별로 안내합니다.

---

## 방법 1: Explore에서 빠르게 확인하기 (가장 쉬움)

### Step 1: Grafana 접속
1. 브라우저에서 `http://localhost:3000` 접속
2. 로그인:
   - **Username**: `admin`
   - **Password**: `admin`

### Step 2: Explore 메뉴 열기
1. 왼쪽 사이드바에서 **Explore** 아이콘 클릭 (🔍 모양)
   - 또는 왼쪽 하단의 **+** 버튼 → **Explore** 클릭

### Step 3: 데이터소스 선택
1. 상단 중앙의 **데이터소스 선택 드롭다운** 클릭
2. **Prometheus** 선택

### Step 4: 쿼리 입력
1. 하단의 **쿼리 입력창**에 다음을 입력:

```promql
{__name__=~"redis_stream.*"}
```

2. 오른쪽의 **Run query** 버튼 클릭 (또는 Shift+Enter)

### Step 5: 결과 확인
- **Table** 탭: 메트릭 목록이 테이블로 표시됨
- **Graph** 탭: 그래프로 표시됨 (값이 있어야 그래프가 그려짐)

### Step 6: 특정 메트릭 쿼리
쿼리 입력창을 비우고 다음 중 하나를 입력:

**A. 평균 Latency 확인:**
```promql
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```

**B. 95th Percentile Latency:**
```promql
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))
```

**C. 처리된 메시지 수:**
```promql
rate(redis_stream_consumer_messages_total[5m])
```

**D. Consumer별 Latency 비교:**
```promql
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```

각 쿼리 입력 후 **Run query** 클릭!

---

## 방법 2: 대시보드에 패널 추가하기 (영구적 모니터링)

### Step 1: 대시보드 열기
1. 왼쪽 사이드바에서 **Dashboards** 클릭
2. **Browse** 클릭
3. 기존 대시보드 선택 (예: `Redis Stream Dashboard`)
   - 또는 **New** → **New dashboard** 클릭하여 새 대시보드 생성

### Step 2: 대시보드 편집 모드 진입
1. 대시보드가 열리면 상단 오른쪽의 **Edit** 버튼 클릭
   - 또는 대시보드 제목 옆의 **⚙️** 아이콘 클릭 → **Edit**

### Step 3: 새 패널 추가
1. 상단의 **Add** 버튼 클릭
2. **Visualization** 선택
   - 또는 빈 공간을 클릭하고 **Add visualization** 클릭

### Step 4: 쿼리 설정
1. 하단 패널에서 **Query** 탭이 선택되어 있는지 확인
2. **Data source**가 **Prometheus**로 선택되어 있는지 확인
3. **Query A** 입력창에 다음 입력:

```promql
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```

4. **Run query** 클릭하여 결과 확인

### Step 5: 패널 설정
1. 오른쪽 패널 설정에서:

**Panel options:**
- **Title**: `Consumer Average Latency` 입력
- **Description**: (선택사항) `Redis Stream Consumer 평균 지연 시간`

**Field:**
- **Unit**: `ms (milliseconds)` 선택
- **Decimals**: `2` (소수점 2자리)

**Legend:**
- **Show legend**: 켜기
- **Legend mode**: `Table` 또는 `List` 선택

### Step 6: 시각화 타입 선택
1. 상단의 **Visualization** 드롭다운에서:
   - **Time series** (기본값) - 시간에 따른 변화 그래프
   - **Stat** - 현재 값만 표시
   - **Table** - 테이블 형태

### Step 7: 패널 저장
1. 오른쪽 상단의 **Apply** 버튼 클릭
2. 대시보드 상단의 **Save** 버튼 클릭
3. 대시보드 이름과 폴더 선택 후 **Save** 클릭

---

## 방법 3: 여러 패널을 한 번에 추가하기

### 패널 1: 평균 Latency (Time Series)
```promql
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```
- **Title**: `Average Latency`
- **Unit**: `ms`
- **Visualization**: `Time series`

### 패널 2: 95th Percentile Latency (Time Series)
```promql
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))
```
- **Title**: `95th Percentile Latency`
- **Unit**: `ms`
- **Visualization**: `Time series`

### 패널 3: 99th Percentile Latency (Time Series)
```promql
histogram_quantile(0.99, rate(redis_stream_consumer_latency_ms_bucket[5m]))
```
- **Title**: `99th Percentile Latency`
- **Unit**: `ms`
- **Visualization**: `Time series`

### 패널 4: 메시지 처리량 (Time Series)
```promql
rate(redis_stream_consumer_messages_total[5m])
```
- **Title**: `Messages Processed per Second`
- **Unit**: `ops/sec`
- **Visualization**: `Time series`

### 패널 5: Consumer별 Latency 비교 (Table)
```promql
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])
```
- **Title**: `Latency by Consumer`
- **Visualization**: `Table`
- **Transform**: (선택사항) `Organize fields`로 컬럼 정렬

---

## 🎨 시각화 팁

### 그래프 색상 변경
1. 패널 편집 모드에서 오른쪽 **Field** 섹션
2. **Override** 클릭
3. **Fields with name** 선택
4. **Color** 설정

### 범례(Legend) 커스터마이징
1. 패널 편집 모드에서 오른쪽 **Legend** 섹션
2. **Show legend**: 켜기
3. **Legend mode**: `Table` (더 많은 정보 표시)
4. **Legend placement**: `Bottom` (그래프 아래)

### 알람(Alert) 설정
1. 패널 편집 모드에서 오른쪽 **Alert** 섹션
2. **Create alert rule from this panel** 클릭
3. 조건 설정:
   - 예: `Average Latency > 1000ms` (1초 초과 시 알람)

---

## 🔍 메트릭이 안 보이는 경우

### 1. 데이터소스 연결 확인
1. 왼쪽 사이드바 **Configuration** (⚙️) → **Data sources**
2. **Prometheus** 클릭
3. **Test** 버튼 클릭
4. **Data source is working** 메시지 확인

### 2. 쿼리 문법 확인
- PromQL 문법이 올바른지 확인
- 메트릭 이름이 정확한지 확인 (`redis_stream_consumer_latency_ms`)

### 3. 시간 범위 확인
- 상단 오른쪽의 **시간 범위** 선택
- **Last 5 minutes** 또는 **Last 1 hour** 선택
- 메트릭이 최근에 수집되었는지 확인

### 4. Prometheus에서 메트릭 확인
1. `http://localhost:9090` 접속
2. Graph 탭에서 동일한 쿼리 입력
3. 결과가 있는지 확인
   - Prometheus에 없으면 Grafana에도 없음

---

## 📝 유용한 PromQL 쿼리 모음

### 기본 쿼리
```promql
# 모든 redis_stream 메트릭
{__name__=~"redis_stream.*"}

# 평균 Latency
rate(redis_stream_consumer_latency_ms_sum[5m]) / rate(redis_stream_consumer_latency_ms_count[5m])

# 50th, 95th, 99th Percentile
histogram_quantile(0.50, rate(redis_stream_consumer_latency_ms_bucket[5m]))
histogram_quantile(0.95, rate(redis_stream_consumer_latency_ms_bucket[5m]))
histogram_quantile(0.99, rate(redis_stream_consumer_latency_ms_bucket[5m]))
```

### 필터링 쿼리
```promql
# 특정 stream_key만
rate(redis_stream_consumer_latency_ms_sum{stream_key="product_action_stream"}[5m]) / rate(redis_stream_consumer_latency_ms_count{stream_key="product_action_stream"}[5m])

# 특정 event_type만
rate(redis_stream_consumer_latency_ms_sum{event_type="product_detail_view"}[5m]) / rate(redis_stream_consumer_latency_ms_count{event_type="product_detail_view"}[5m])
```

### 집계 쿼리
```promql
# 모든 Consumer의 평균 Latency 합계
sum(rate(redis_stream_consumer_latency_ms_sum[5m])) / sum(rate(redis_stream_consumer_latency_ms_count[5m]))

# Consumer별로 그룹화
sum by (stream_key) (rate(redis_stream_consumer_latency_ms_sum[5m])) / sum by (stream_key) (rate(redis_stream_consumer_latency_ms_count[5m]))
```

---

## 🚀 빠른 시작 체크리스트

- [ ] Grafana 접속: `http://localhost:3000` (admin/admin)
- [ ] Explore 메뉴 열기
- [ ] 데이터소스: Prometheus 선택
- [ ] 쿼리 입력: `{__name__=~"redis_stream.*"}`
- [ ] Run query 클릭
- [ ] 결과 확인

**결과가 나오면** → 메트릭이 정상적으로 수집되고 있습니다! 🎉

**결과가 안 나오면** → `RECOMMEND_SERVER_CONNECTION_FIX.md` 파일 참고

