#!/bin/bash
# Redis Cluster에서 Stream 확인 스크립트

STREAM_NAME="${1:-product_action_stream}"

echo "═══════════════════════════════════════════════════════════════"
echo "🔍 Redis Stream 확인: $STREAM_NAME"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# 모든 노드에서 Stream 확인
for i in 1 2 3 4 5 6; do
    port=$((7005 + i))
    echo "📌 redis-node-$i:$port"
    echo "─────────────────────────────────────────────────────────────"
    
    # 클러스터 모드로 Stream 정보 확인
    result=$(docker exec -it redis-node-$i redis-cli -a 1234 -p $port -c XINFO STREAM $STREAM_NAME 2>&1)
    
    if echo "$result" | grep -q "MOVED"; then
        # MOVED 응답에서 올바른 노드 정보 추출
        moved_node=$(echo "$result" | grep -oP 'MOVED \d+ \K[0-9.]+:\d+')
        echo "  ⚠️  키가 다른 노드에 있습니다: $moved_node"
        echo "  💡 올바른 노드에서 확인하세요:"
        echo "     docker exec -it redis-node-3 redis-cli -a 1234 -p 7008 -c XINFO STREAM $STREAM_NAME"
    elif echo "$result" | grep -q "no such key"; then
        echo "  ❌ Stream이 아직 생성되지 않았습니다."
    elif echo "$result" | grep -q "length"; then
        echo "  ✅ Stream 발견!"
        echo "$result" | head -10
    else
        echo "  ❌ Stream 없음 또는 에러"
        echo "$result" | head -3
    fi
    echo ""
done

echo "═══════════════════════════════════════════════════════════════"
echo "💡 사용법:"
echo "   ./check_redis_stream.sh [stream_name]"
echo "   예: ./check_redis_stream.sh product_action_stream"
echo "═══════════════════════════════════════════════════════════════"

