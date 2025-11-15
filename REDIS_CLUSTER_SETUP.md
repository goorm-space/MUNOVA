# Redis Cluster 초기 설정 가이드

## 📋 개요

이 프로젝트는 Redis를 두 가지 용도로 분리하여 사용합니다:
- **redis-standalone**: 서비스 처리용 (Refresh Token, 분산락 등)
- **redis-cluster**: 로그 파이프라인용 (대량 로그 스트림 처리)

## 🚀 초기 설정 방법

### 1. Docker Compose로 Redis 컨테이너 시작

```bash
# Redis 컨테이너만 시작
docker-compose up -d redis-standalone redis-node-1 redis-node-2 redis-node-3 redis-node-4 redis-node-5 redis-node-6
```

### 2. Redis Cluster 초기화

Redis Cluster는 컨테이너만 실행되는 것으로는 작동하지 않습니다. 반드시 초기화 스크립트를 실행해야 합니다.

```bash
# 초기화 스크립트 실행
chmod +x init-redis-cluster.sh
./init-redis-cluster.sh
```

### 3. 초기화 스크립트 내용

`init-redis-cluster.sh` 스크립트는 다음 작업을 수행합니다:

1. 모든 노드의 데이터 삭제 (`FLUSHALL`)
2. 모든 노드 리셋 (`CLUSTER RESET HARD`)
3. Cluster 생성 (`redis-cli --cluster create`)
4. Cluster 상태 확인

### 4. Cluster 상태 확인

```bash
# Cluster 상태 확인
docker exec redis-node-1 redis-cli -a 1234 -p 7006 CLUSTER INFO

# Cluster 노드 확인
docker exec redis-node-1 redis-cli -a 1234 -p 7006 CLUSTER NODES
```

정상 상태:
- `cluster_state:ok`
- `cluster_slots_ok:16384`
- `cluster_slots_fail:0`

## 🔐 Redis Password 설정

모든 Redis 인스턴스는 password `1234`로 설정되어 있습니다.

### Password 확인

```bash
# redis-standalone
docker exec redis-standalone redis-cli -a 1234 PING

# redis-cluster (각 노드)
docker exec redis-node-1 redis-cli -a 1234 -p 7006 PING
```

## 📝 주의사항

### 1. 데이터 초기화

- `docker-compose down -v` 실행 시 모든 Redis 데이터가 삭제됩니다
- Cluster를 재초기화하려면 `init-redis-cluster.sh`를 다시 실행해야 합니다

### 2. Cluster 재초기화가 필요한 경우

다음 상황에서 Cluster 재초기화가 필요합니다:
- `docker-compose down -v` 후 재시작
- Cluster 상태가 `fail`로 변경된 경우
- 노드 추가/제거 후

### 3. Volume 삭제 후 재시작

```bash
# 1. 컨테이너 중지
docker-compose down

# 2. Redis volumes 삭제 (선택사항 - 데이터 초기화)
docker volume rm munova_redis_node_1_data \
  munova_redis_node_2_data \
  munova_redis_node_3_data \
  munova_redis_node_4_data \
  munova_redis_node_5_data \
  munova_redis_node_6_data

# 3. 컨테이너 재시작
docker-compose up -d redis-standalone redis-node-1 redis-node-2 redis-node-3 redis-node-4 redis-node-5 redis-node-6

# 4. Cluster 초기화
./init-redis-cluster.sh
```

## 🔧 트러블슈팅

### 문제: `CLUSTERDOWN The cluster is down`

**원인**: Cluster가 초기화되지 않았거나 상태가 불안정함

**해결**:
```bash
./init-redis-cluster.sh
```

### 문제: `NOAUTH Authentication required`

**원인**: Password가 설정되지 않았거나 잘못된 password 사용

**해결**: 
- `application-docker.properties`에서 `spring.data.redis.password=1234` 확인
- Redis 컨테이너가 `--requirepass 1234` 옵션으로 실행되었는지 확인

### 문제: `MOVED redirection loop detected`

**원인**: Cluster 노드에 standalone 모드로 연결 시도

**해결**: 
- `redis-standalone`을 사용하도록 설정 확인
- `application-docker.properties`에서 `spring.data.redis.host=redis-standalone` 확인

## 📊 Redis 구조

```
┌─────────────────┐
│  munova-api     │
└────────┬────────┘
         │
    ┌────┴────┐
    │         │
    ▼         ▼
┌─────────┐ ┌──────────────┐
│ Standalone│ │   Cluster    │
│ (6379)   │ │ (7006-7011)  │
│          │ │              │
│ 서비스   │ │ 로그 파이프라인│
│ 처리     │ │              │
└─────────┘ └──────────────┘
```

## 🔗 관련 파일

- `docker-compose.yml`: Redis 컨테이너 설정
- `init-redis-cluster.sh`: Cluster 초기화 스크립트
- `src/main/resources/application-docker.properties`: Redis 연결 설정
- `src/main/java/com/space/munova/core/config/RedisConfig.java`: Redis Bean 설정
- `src/main/java/com/space/munova/core/config/RedissonConfig.java`: Redisson 설정

## ✅ 초기 설정 체크리스트

- [ ] Docker Compose로 Redis 컨테이너 시작
- [ ] `init-redis-cluster.sh` 실행
- [ ] Cluster 상태 확인 (`cluster_state:ok`)
- [ ] 애플리케이션 빌드 및 실행
- [ ] Health Check에서 Redis 상태 확인
- [ ] 로그인 테스트 (Refresh Token 저장 확인)

