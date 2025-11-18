#!/bin/bash
# Redis Stream 내부 메시지만 삭제 (Stream은 유지)
# Redis Cluster 모드에서 작동하도록 최적화

set -e  # 에러 발생 시 중단

echo "═══════════════════════════════════════════════════════════════"
echo "🗑️  Redis Stream 메시지 삭제 시작 (Stream은 유지)"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# 삭제할 Stream 목록 (user_action_stream_0 ~ 9만 사용)
STREAMS=(
    "user_action_stream_0"
    "user_action_stream_1"
    "user_action_stream_2"
    "user_action_stream_3"
    "user_action_stream_4"
    "user_action_stream_5"
    "user_action_stream_6"
    "user_action_stream_7"
    "user_action_stream_8"
    "user_action_stream_9"
    "user_action_stream_unknown"
)

# Redis Cluster는 하나의 노드에서 실행해도 자동으로 올바른 노드로 리다이렉션됨
REDIS_NODE="redis-node-1"
REDIS_PORT="7006"
PASSWORD="1234"

total_deleted=0
total_streams=0

for stream in "${STREAMS[@]}"; do
    echo "📌 Stream: $stream"
    echo "─────────────────────────────────────────────────────────────"
    
    # Stream 길이 확인 (클러스터 모드로 자동 리다이렉션)
    length_result=$(docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XLEN "$stream" 2>&1 | grep -v "Warning" | tr -d '\r\n')
    
    if [[ "$length_result" =~ ^[0-9]+$ ]]; then
        length=$length_result
        if [ "$length" -gt 0 ]; then
            echo "  📊 현재 메시지 수: $length개"
            
            # XTRIM으로 모든 메시지 삭제 (MAXLEN 0 = 모든 메시지 삭제)
            # 클러스터 모드에서는 자동으로 올바른 노드로 리다이렉션됨
            trim_result=$(docker exec $REDIS_NODE redis-cli -a $PASSWORD -p $REDIS_PORT -c XTRIM "$stream" MAXLEN 0 2>&1 | grep -v "Warning" | tr -d '\r\n')
            
            if [[ "$trim_result" =~ ^[0-9]+$ ]]; then
                deleted_count=$trim_result
                total_deleted=$((total_deleted + deleted_count))
                total_streams=$((total_streams + 1))
                echo "  ✅ 메시지 $deleted_count개 삭제 완료"
            else
                echo "  ⚠️  삭제 실패: $trim_result"
            fi
        else
            echo "  ℹ️  메시지 없음 (이미 비어있음)"
        fi
    elif echo "$length_result" | grep -qi "no such key\|not found"; then
        echo "  ℹ️  Stream 없음 (생성되지 않음)"
    else
        echo "  ⚠️  확인 실패: $length_result"
    fi
    
    echo ""
done

echo "═══════════════════════════════════════════════════════════════"
echo "✅ Stream 메시지 삭제 완료"
echo "   총 삭제된 메시지: $total_deleted개"
echo "   처리된 Stream: $total_streams개"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "💡 Stream 자체는 유지되며, 내부 메시지만 삭제되었습니다."
echo "   Stream을 완전히 삭제하려면 DEL 명령을 사용하세요."

