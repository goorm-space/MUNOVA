#!/bin/bash
# k6 테스트 시작 시 Grafana에 빨간색 기준선(Annotation) 추가

GRAFANA_URL="http://localhost:3000"
GRAFANA_USER="admin"
GRAFANA_PASSWORD="admin"

# 현재 시간을 타임스탬프로 변환 (밀리초)
TIMESTAMP=$(date +%s)000

# Annotation 추가
curl -X POST \
  -H "Content-Type: application/json" \
  -u "${GRAFANA_USER}:${GRAFANA_PASSWORD}" \
  -d "{
    \"dashboardId\": null,
    \"dashboardUID\": \"redis-stream\",
    \"panelId\": null,
    \"time\": ${TIMESTAMP},
    \"timeEnd\": ${TIMESTAMP},
    \"tags\": [\"test-start\"],
    \"text\": \"🚀 k6 부하 테스트 시작\"
  }" \
  "${GRAFANA_URL}/api/annotations" 2>/dev/null

echo "✅ 테스트 시작 Annotation 추가 완료 (${TIMESTAMP})"

