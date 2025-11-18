#!/bin/bash
# Redis Stream 데이터 확인 스크립트 (실시간 모니터링)

STREAM_NAME="${1:-product_action_stream}"
LIMIT="${2:-10}"  # 기본 10개 메시지 조회

echo "═══════════════════════════════════════════════════════════════"
echo "🔍 Redis Stream 데이터 확인: $STREAM_NAME"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# Redis Cluster는 하나의 노드에서 실행해도 자동으로 올바른 노드로 리다이렉션됨
REDIS_NODE="redis-node-1"
REDIS_PORT="7006"
PASSWORD="1234"

# Stream 길이 확인
echo "📊 Stream 길이 확인..."
length=$(docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XLEN "$STREAM_NAME" 2>&1 | grep -v "Warning" | tr -d '\r\n')

if [[ "$length" =~ ^[0-9]+$ ]]; then
    echo "  ✅ Stream에 메시지 $length 개가 있습니다"
    echo ""
    
    if [ "$length" -gt 0 ]; then
        echo "📝 최근 메시지 $LIMIT 개 조회..."
        echo "─────────────────────────────────────────────────────────────"
        
        # 최근 메시지 읽기 (XREAD COUNT)
        docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XREVRANGE "$STREAM_NAME" + - COUNT $LIMIT 2>&1 | grep -v "Warning"
        
        echo ""
        echo "─────────────────────────────────────────────────────────────"
        echo "💡 전체 메시지 조회:"
        echo "   docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XREVRANGE $STREAM_NAME + -"
        echo ""
        echo "💡 Stream 정보 상세 확인:"
        echo "   docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XINFO STREAM $STREAM_NAME"
    else
        echo "  ℹ️  Stream은 존재하지만 메시지가 없습니다"
    fi
else
    echo "  ❌ Stream이 아직 생성되지 않았거나 에러 발생"
    echo "  응답: $length"
    echo ""
    echo "💡 사용 가능한 Stream 목록 확인:"
    echo "   docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c KEYS '*stream*'"
fi

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "💡 사용법:"
echo "   ./check_stream_data.sh [stream_name] [limit]"
echo "   예: ./check_stream_data.sh product_action_stream 20"
echo "═══════════════════════════════════════════════════════════════"

