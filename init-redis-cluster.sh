#!/bin/bash

# Redis Cluster 초기화 스크립트 (Password 포함)

REDIS_PASSWORD="1234"

echo "=== Redis Cluster 초기화 시작 (Password: $REDIS_PASSWORD) ==="

# 1. 모든 노드의 데이터 삭제 및 리셋
echo "1. 모든 노드 리셋 중..."
docker exec redis-node-1 redis-cli -a $REDIS_PASSWORD -p 7006 FLUSHALL
docker exec redis-node-2 redis-cli -a $REDIS_PASSWORD -p 7007 FLUSHALL
docker exec redis-node-3 redis-cli -a $REDIS_PASSWORD -p 7008 FLUSHALL
docker exec redis-node-4 redis-cli -a $REDIS_PASSWORD -p 7009 FLUSHALL
docker exec redis-node-5 redis-cli -a $REDIS_PASSWORD -p 7010 FLUSHALL
docker exec redis-node-6 redis-cli -a $REDIS_PASSWORD -p 7011 FLUSHALL

docker exec redis-node-1 redis-cli -a $REDIS_PASSWORD -p 7006 CLUSTER RESET HARD
docker exec redis-node-2 redis-cli -a $REDIS_PASSWORD -p 7007 CLUSTER RESET HARD
docker exec redis-node-3 redis-cli -a $REDIS_PASSWORD -p 7008 CLUSTER RESET HARD
docker exec redis-node-4 redis-cli -a $REDIS_PASSWORD -p 7009 CLUSTER RESET HARD
docker exec redis-node-5 redis-cli -a $REDIS_PASSWORD -p 7010 CLUSTER RESET HARD
docker exec redis-node-6 redis-cli -a $REDIS_PASSWORD -p 7011 CLUSTER RESET HARD

echo "2. Cluster 생성 중 (Password 포함)..."
docker exec redis-node-1 redis-cli --cluster create \
  redis-node-1:7006 \
  redis-node-2:7007 \
  redis-node-3:7008 \
  redis-node-4:7009 \
  redis-node-5:7010 \
  redis-node-6:7011 \
  --cluster-replicas 1 \
  --cluster-yes \
  -a $REDIS_PASSWORD

echo "3. Cluster 상태 확인..."
docker exec redis-node-1 redis-cli -a $REDIS_PASSWORD -p 7006 CLUSTER INFO

echo "=== Redis Cluster 초기화 완료 ==="

