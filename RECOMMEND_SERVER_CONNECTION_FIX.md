# 🚨 추천 서버 연결 문제 해결 가이드

## ❌ 문제 상황

```bash
curl http://localhost:8002/metrics
# curl: (7) Failed to connect to localhost port 8002
```

**원인**: `docker-compose.yml`에 추천 서버가 정의되어 있지 않아서 포트가 매핑되지 않았습니다.

---

## ✅ 해결 방법

### 방법 1: Prometheus 컨테이너 내부에서 확인 (권장)

Prometheus는 도커 네트워크 내부에서 `recommend:8001`, `recommend:8002` 형식으로 접근합니다.

```bash
# Prometheus 컨테이너 내부에서 추천 서버 메트릭 확인
docker exec prometheus wget -qO- http://recommend:8001/metrics | grep redis_stream

# Consumer 메트릭 확인
docker exec prometheus wget -qO- http://recommend:8002/metrics | grep redis_stream
```

**또는 스크립트 실행:**
```bash
./check_recommend_metrics.sh
```

---

### 방법 2: Prometheus UI에서 확인 (가장 확실한 방법)

1. **브라우저에서 접속**: `http://localhost:9090`
2. **Status → Targets** 메뉴 클릭
3. 다음 항목들이 **UP** 상태인지 확인:
   - `recommend-server` 
   - `recommend-consumers` (10개)

**UP 상태라면** → Prometheus가 메트릭을 수집하고 있는 것입니다!

**DOWN 상태라면** → 추천 서버가 실행되지 않았거나 네트워크 문제입니다.

---

### 방법 3: Prometheus에서 직접 쿼리

1. **브라우저에서 접속**: `http://localhost:9090`
2. **Graph** 탭에서 검색창에 입력:

```promql
{__name__=~"redis_stream.*"}
```

3. **Execute** 클릭
4. 결과가 나오면 → 메트릭이 수집되고 있는 것입니다!

---

### 방법 4: 추천 서버 포트를 호스트에 매핑 (선택사항)

만약 `localhost:8002`로 직접 접근하고 싶다면, 추천 서버를 별도로 실행하거나 `docker-compose.yml`에 추가해야 합니다.

**주의**: 추천 서버가 별도 프로젝트라면, 그 프로젝트의 `docker-compose.yml`에서 포트를 매핑해야 합니다.

---

## 🔍 진단 체크리스트

### 1. 추천 서버가 실행 중인가?

```bash
# 도커 컨테이너 확인
docker ps | grep recommend

# 또는 모든 컨테이너 확인
docker ps -a | grep recommend
```

**결과가 없으면** → 추천 서버가 실행되지 않았습니다.

---

### 2. Prometheus가 추천 서버에 접근할 수 있는가?

```bash
# Prometheus 컨테이너에서 테스트
docker exec prometheus ping -c 2 recommend
```

**ping이 실패하면** → 네트워크 문제 또는 추천 서버가 다른 네트워크에 있습니다.

---

### 3. Prometheus Targets 상태 확인

**브라우저에서**: `http://localhost:9090/targets`

- ✅ **UP** (녹색) → 정상 작동 중
- ❌ **DOWN** (빨간색) → 문제 있음
  - 에러 메시지를 확인하세요
  - "connection refused" → 추천 서버가 실행되지 않음
  - "timeout" → 네트워크 문제

---

### 4. 추천 서버 메트릭 엔드포인트 확인

**Prometheus 컨테이너 내부에서:**
```bash
docker exec prometheus wget -qO- http://recommend:8001/metrics 2>&1 | head -20
```

**응답이 있으면** → 메트릭은 노출되고 있습니다.
**응답이 없으면** → 추천 서버가 실행되지 않았거나 메트릭을 노출하지 않습니다.

---

## 💡 핵심 정리

### ❌ 잘못된 접근 방법
```bash
curl http://localhost:8002/metrics  # ❌ 작동 안 함
```
**이유**: 추천 서버가 `docker-compose.yml`에 없어서 포트가 매핑되지 않음

### ✅ 올바른 접근 방법

1. **Prometheus UI에서 확인** (가장 확실)
   - `http://localhost:9090` → Targets 또는 Graph

2. **Prometheus 컨테이너 내부에서 확인**
   ```bash
   docker exec prometheus wget -qO- http://recommend:8002/metrics
   ```

3. **Grafana Explore에서 확인**
   - `http://localhost:3000` → Explore → Prometheus 선택 → 쿼리 입력

---

## 🎯 다음 단계

1. **Prometheus Targets 확인**: `http://localhost:9090/targets`
   - `recommend-server`, `recommend-consumers`가 UP인지 확인

2. **Prometheus에서 메트릭 쿼리**: `http://localhost:9090/graph`
   - `{__name__=~"redis_stream.*"}` 입력

3. **Grafana Explore에서 확인**: `http://localhost:3000`
   - Explore → Prometheus → 쿼리 입력

**이 방법들로 확인이 안 되면** → 추천 서버가 실행되지 않았거나 메트릭을 노출하지 않는 것입니다.

---

## 📝 참고사항

- **Prometheus는 도커 네트워크 내부에서만 접근 가능합니다**
- `prometheus.yml`에서 `recommend:8001` 형식으로 설정한 것은 도커 네트워크 내부 주소입니다
- `localhost:8001`은 호스트 포트가 매핑되어야 접근 가능합니다
- 추천 서버가 별도 프로젝트라면, 그 프로젝트에서 포트를 매핑해야 합니다

