#!/bin/bash

echo "🔍 추천 서버 메트릭 확인 스크립트"
echo "=================================="
echo ""

# 1. Prometheus 컨테이너 내부에서 추천 서버 접근 확인
echo "1️⃣ Prometheus 컨테이너에서 추천 서버 메트릭 확인"
echo "------------------------------------------------"
docker exec prometheus wget -qO- http://recommend:8001/metrics 2>/dev/null | grep redis_stream | head -10
if [ $? -eq 0 ]; then
    echo "✅ recommend:8001 접근 성공"
else
    echo "❌ recommend:8001 접근 실패"
fi
echo ""

# 2. Consumer 메트릭 확인 (예시)
echo "2️⃣ Consumer 메트릭 확인 (recommend:8002)"
echo "------------------------------------------------"
docker exec prometheus wget -qO- http://recommend:8002/metrics 2>/dev/null | grep redis_stream | head -10
if [ $? -eq 0 ]; then
    echo "✅ recommend:8002 접근 성공"
else
    echo "❌ recommend:8002 접근 실패"
fi
echo ""

# 3. Prometheus Targets 상태 확인
echo "3️⃣ Prometheus Targets 상태"
echo "------------------------------------------------"
echo "브라우저에서 확인: http://localhost:9090/targets"
echo ""

# 4. 실행 중인 추천 서버 컨테이너 확인
echo "4️⃣ 실행 중인 추천 서버 컨테이너"
echo "------------------------------------------------"
docker ps | grep recommend || echo "❌ 추천 서버 컨테이너가 실행 중이지 않습니다"
echo ""

# 5. Prometheus 설정 확인
echo "5️⃣ Prometheus 설정 확인"
echo "------------------------------------------------"
docker exec prometheus cat /etc/prometheus/prometheus.yml | grep -A 5 "recommend-server" || echo "❌ recommend-server 설정이 없습니다"
echo ""

echo "=================================="
echo "💡 참고사항:"
echo "- 추천 서버는 도커 네트워크 내부에서만 접근 가능합니다"
echo "- Prometheus는 'recommend:8001' 형식으로 접근합니다"
echo "- localhost:8001은 호스트 포트가 매핑되어야 접근 가능합니다"
echo ""

