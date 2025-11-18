# 메트릭 확인 단계별 가이드

## 1단계: Prometheus 엔드포인트 확인

브라우저에서 접속:
```
http://localhost:8080/actuator/prometheus
```

다음 메트릭을 검색해보세요:
- `redis_stream_buffer_size` (버퍼 크기 - 즉시 생성되어야 함)
- `redis_stream_buffer_capacity` (버퍼 용량 - 즉시 생성되어야 함)

## 2단계: Prometheus에서 확인

Prometheus 접속: `http://localhost:9090`

상단 검색창에 입력:
```
redis_stream_buffer_size
```

메트릭이 보이면 정상, 안 보이면 문제가 있는 것입니다.

## 3단계: 애플리케이션 로그 확인

```bash
docker-compose logs munova-api | grep -i "metric\|redis.stream\|LogBatchBuffer"
```

메트릭 초기화 로그가 있는지 확인하세요.

## 4단계: 간단한 API 호출로 메트릭 생성

버퍼 메트릭은 즉시 보여야 하지만, Counter/Timer 메트릭은 실제 API 호출이 필요합니다.

```bash
# 상품 상세조회 API 호출 (로그 전송 트리거)
curl -H "Authorization: Bearer YOUR_TOKEN" \
  http://localhost:8080/api/product/1
```

호출 후 `/actuator/prometheus`에서 확인:
- `redis_stream_send_success_total`
- `redis_stream_send_duration_seconds`

## 5단계: Grafana 쿼리 확인

Grafana에서 각 패널의 쿼리를 직접 확인:
1. 패널 클릭 → Edit
2. Query 탭에서 쿼리 확인
3. "Run query" 버튼 클릭하여 직접 테스트

## 문제 해결

### 버퍼 메트릭도 안 보이는 경우
- 애플리케이션이 재시작되지 않았을 수 있음
- `docker-compose restart munova-api` 실행

### Counter/Timer 메트릭만 안 보이는 경우
- 정상입니다. 실제 API 호출이 필요합니다.
- 위의 4단계를 실행하세요.

