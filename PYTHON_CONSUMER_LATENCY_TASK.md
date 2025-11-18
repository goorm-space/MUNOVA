# 📌 Redis Stream Latency 측정 구현 작업 (파이썬 Consumer)

## 🎯 작업 목표

API 서버 → Redis Stream → 추천 서버(Consumer)까지의 **전체 지연 시간(end-to-end latency)**을 측정하기 위해, Consumer에서 메시지를 읽을 때 latency를 계산하고 로그로 출력하며, Prometheus metrics로 노출할 수 있도록 구현합니다.

---

## ✅ API 서버에서 이미 완료된 작업

API 서버(Spring Boot)에서는 이미 다음 작업이 완료되었습니다:

1. **`producer_time` 필드 추가 완료**
   - `RedisStreamProducer.sendLogAsync()` 메서드에서 모든 Redis Stream 메시지에 `producer_time` 필드를 추가했습니다.
   - 값은 `System.currentTimeMillis()`로 설정되어 밀리초 단위 타임스탬프입니다.
   - 모든 메시지의 최상위 레벨에 `producer_time` 필드가 포함됩니다.

2. **메시지 형식**
   - Redis Stream에 저장되는 메시지에는 다음과 같은 필드들이 포함됩니다:
     ```
     {
       "producer_time": "1704067200000",  // 밀리초 단위 (새로 추가됨)
       "event_time": "2024-01-01T00:00:00Z",
       "event_timestamp": "1704067200000",
       "session_id": "uuid-string",
       "version": "1",
       "stream_key": "product_action_stream",
       "event_type": "product_detail_view",
       "service": "product",
       "member_id": "12345",
       "data.product_id": "67890",
       ... 기타 필드들
     }
     ```

---

## 🔧 파이썬 Consumer에서 구현해야 할 작업

### 1. 메시지 컨슘 코드 수정

Redis Stream에서 메시지를 읽는 Consumer 코드를 찾아서 다음 로직을 추가해야 합니다:

#### A. Latency 계산 로직 추가

메시지를 읽은 직후, `producer_time` 필드가 있는지 확인하고 latency를 계산합니다:

```python
import time
from typing import Dict, Any

def process_message(message: Dict[str, Any]) -> None:
    """
    Redis Stream에서 읽은 메시지를 처리하는 함수
    
    Args:
        message: Redis Stream에서 읽은 메시지 (fields 딕셔너리 포함)
    """
    # 메시지 fields 추출 (Redis Stream 메시지 구조에 맞게 수정 필요)
    fields = message.get('fields', {})  # 또는 message.get('data', {}) 등 실제 구조에 맞게
    
    # producer_time 필드 확인 및 latency 계산
    if 'producer_time' in fields:
        try:
            # 현재 시간 (밀리초)
            now_ms = int(time.time() * 1000)
            
            # Producer가 메시지를 보낸 시간 (밀리초)
            producer_ms = int(fields.get('producer_time'))
            
            # Latency 계산 (Consumer가 메시지를 읽은 시점 - Producer가 보낸 시점)
            latency_ms = now_ms - producer_ms
            
            # 로그 출력
            print(f"[LATENCY] {latency_ms} ms | producer_time={producer_ms} | consumer_time={now_ms}")
            
            # 또는 로깅 라이브러리 사용 (예: logging)
            # logger.info(f"[LATENCY] {latency_ms} ms | producer_time={producer_ms} | consumer_time={now_ms}")
            
        except (ValueError, TypeError) as e:
            print(f"[ERROR] Latency 계산 실패: {e} | fields={fields}")
    else:
        # producer_time이 없는 경우 (구버전 메시지일 수 있음)
        print(f"[WARN] producer_time 필드가 없습니다. 메시지: {fields}")
    
    # 기존 메시지 처리 로직 계속 실행
    # ... 기존 코드 ...
```

#### B. Prometheus Metrics 노출 (선택사항)

Prometheus exporter를 사용하여 latency를 metrics로 노출하려면:

```python
from prometheus_client import Histogram, Counter
import time

# Prometheus metrics 정의 (모듈 레벨에서 한 번만 정의)
latency_histogram = Histogram(
    'redis_stream_consumer_latency_ms',
    'Redis Stream Consumer Latency (milliseconds)',
    ['stream_key', 'event_type'],  # 라벨 추가 가능
    buckets=[10, 50, 100, 200, 500, 1000, 2000, 5000, 10000]  # 히스토그램 버킷
)

latency_counter = Counter(
    'redis_stream_consumer_messages_total',
    'Total number of messages consumed from Redis Stream',
    ['stream_key', 'has_producer_time']
)

def process_message_with_metrics(message: Dict[str, Any]) -> None:
    """
    Metrics를 포함한 메시지 처리 함수
    """
    fields = message.get('fields', {})
    stream_key = fields.get('stream_key', 'unknown')
    event_type = fields.get('event_type', 'unknown')
    
    if 'producer_time' in fields:
        try:
            now_ms = int(time.time() * 1000)
            producer_ms = int(fields.get('producer_time'))
            latency_ms = now_ms - producer_ms
            
            # Latency 히스토그램에 기록
            latency_histogram.labels(
                stream_key=stream_key,
                event_type=event_type
            ).observe(latency_ms)
            
            # 메시지 카운터 증가 (producer_time 있음)
            latency_counter.labels(
                stream_key=stream_key,
                has_producer_time='true'
            ).inc()
            
            print(f"[LATENCY] {latency_ms} ms | stream={stream_key} | event={event_type}")
            
        except (ValueError, TypeError) as e:
            print(f"[ERROR] Latency 계산 실패: {e}")
            latency_counter.labels(
                stream_key=stream_key,
                has_producer_time='error'
            ).inc()
    else:
        # producer_time이 없는 경우
        latency_counter.labels(
            stream_key=stream_key,
            has_producer_time='false'
        ).inc()
    
    # 기존 메시지 처리 로직 계속 실행
    # ... 기존 코드 ...
```

---

## 📋 구현 체크리스트

- [ ] Redis Stream Consumer 코드 위치 확인
- [ ] 메시지 읽기 루프에서 `producer_time` 필드 확인 로직 추가
- [ ] Latency 계산 로직 구현 (`now_ms - producer_ms`)
- [ ] Latency 로그 출력 (최소한 print 또는 logger 사용)
- [ ] (선택) Prometheus Histogram metrics 추가
- [ ] (선택) Prometheus Counter metrics 추가 (메시지 수 추적)
- [ ] 예외 처리 추가 (producer_time이 없거나 잘못된 형식인 경우)
- [ ] 테스트: 실제 메시지를 읽어서 latency가 정상적으로 계산되는지 확인

---

## 🔍 구현 시 주의사항

1. **메시지 구조 확인 필요**
   - Redis Stream 메시지의 실제 구조를 확인해야 합니다.
   - `redis-py`를 사용하는 경우, `XREAD` 결과는 보통 `[{stream_key: [(id, {fields...})]}]` 형태입니다.
   - 실제 코드에 맞게 `fields` 추출 방식을 수정해야 합니다.

2. **시간 동기화**
   - Producer와 Consumer 서버 간 시간 동기화가 중요합니다.
   - NTP를 사용하여 서버 시간을 동기화하는 것을 권장합니다.
   - 시간이 동기화되지 않은 경우 latency 값이 음수로 나올 수 있습니다.

3. **예외 처리**
   - `producer_time` 필드가 없는 경우 (구버전 메시지)
   - `producer_time` 값이 숫자가 아닌 경우
   - 계산 결과가 음수인 경우 (시간 동기화 문제)

4. **성능 고려**
   - Latency 계산은 매우 가벼운 연산이므로 성능에 큰 영향은 없습니다.
   - 하지만 메시지 처리량이 매우 높다면, 로깅을 비동기로 처리하거나 샘플링을 고려할 수 있습니다.

---

## 📊 예상 결과

구현이 완료되면:

1. **로그 출력 예시:**
   ```
   [LATENCY] 45 ms | producer_time=1704067200000 | consumer_time=1704067200045
   [LATENCY] 32 ms | producer_time=1704067200100 | consumer_time=1704067200132
   ```

2. **Prometheus Metrics (구현한 경우):**
   - `redis_stream_consumer_latency_ms` 히스토그램: latency 분포 확인 가능
   - `redis_stream_consumer_messages_total` 카운터: 처리된 메시지 수 추적

3. **Grafana 대시보드 (선택사항):**
   - Prometheus metrics를 Grafana에서 시각화하여 실시간 latency 모니터링 가능

---

## ❓ 질문이 있거나 도움이 필요한 경우

- Redis Stream 메시지 구조가 위 예시와 다른 경우, 실제 메시지 구조를 공유해주시면 코드를 수정해드릴 수 있습니다.
- Prometheus exporter 설정이 필요한 경우, 현재 프로젝트의 Prometheus 설정을 확인하여 일관성 있게 구현하세요.

