#!/bin/bash
# Redis Stream 실시간 모니터링 스크립트

STREAM_NAME="${1:-product_action_stream}"
INTERVAL="${2:-2}"  # 기본 2초마다 확인

echo "═══════════════════════════════════════════════════════════════"
echo "📡 Redis Stream 실시간 모니터링: $STREAM_NAME"
echo "   업데이트 간격: ${INTERVAL}초"
echo "   종료: Ctrl+C"
echo "═══════════════════════════════════════════════════════════════"
echo ""

REDIS_NODE="redis-node-1"
REDIS_PORT="7006"
PASSWORD="1234"

last_length=0

while true; do
    # Stream 길이 확인
    length=$(docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XLEN "$STREAM_NAME" 2>&1 | grep -v "Warning" | tr -d '\r\n')
    
    if [[ "$length" =~ ^[0-9]+$ ]]; then
        current_time=$(date '+%Y-%m-%d %H:%M:%S')
        
        if [ "$length" -gt "$last_length" ]; then
            new_messages=$((length - last_length))
            echo "[$current_time] 📈 메시지 증가: $last_length → $length (+$new_messages개) | 총: $length개"
            
            # 최근 메시지 1개만 보여주기
            if [ "$new_messages" -gt 0 ]; then
                echo "   최근 메시지:"
                docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XREVRANGE "$STREAM_NAME" + - COUNT 1 2>&1 | grep -v "Warning" | head -5 | sed 's/^/   /'
            fi
        elif [ "$length" -eq "$last_length" ]; then
            echo "[$current_time] ⏸️  변화 없음: $length개"
        else
            echo "[$current_time] ⚠️  메시지 감소: $last_length → $length"
        fi
        
        last_length=$length
    else
        echo "[$(date '+%Y-%m-%d %H:%M:%S')] ❌ Stream 확인 실패: $length"
    fi
    
    sleep $INTERVAL
done

