// 3개의 유저가 다양한 활동 추적
import http from 'k6/http';
import { sleep, check } from 'k6';

// --- 액세스 토큰 (예시용, 실제 테스트 시 여러 사용자 토큰 리스트로 교체 가능)
const TOKENS = [
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczEiLCJpYXQiOjE3NjMwMjIzMzQsImV4cCI6MTc2MzIwMjMzNH0.ZTqlLd1QOh5zazeMm_O0iR28woi_AX6ygko4Z1sb8pwZUPYmPvvkgejx2n5oXm3w5j7TR3wHBQbTAV_dqLtDjA',
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczIiLCJpYXQiOjE3NjMwMjIzNTIsImV4cCI6MTc2MzIwMjM1Mn0.TFdiOjupvxEzfFrPKYdg_aQl8SXDKyTNosiQ71fw2blCzG_pWURTxPZ5jWQheMJF1tc20IkY_R6rGdLdyzKaMQ', // 예시
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI3IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczMiLCJpYXQiOjE3NjMwMjIzNjQsImV4cCI6MTc2MzIwMjM2NH0.qrvGbPBgKjDzKtttX8KttEK5V-jgBOxGWO_rsCusvjPcGPXqGvO6jl3aff3de9c443pK-5xghWXVul2hmEa-Cg'  // 예시
];

// --- 부하 설정
export const options = {
    vus: 10000,        // 가상의 사용자 (동시 접속자)
    duration: '10s',   // 테스트 지속 시간
    thresholds: {
        http_req_failed: ['rate<0.05'],    // 실패율 5% 이하 목표
        http_req_duration: ['p(95)<2000'], // 95% 요청 2초 이내
    },
};

// --- 상품 ID 풀 (예: 1~1000)
const PRODUCT_IDS = Array.from({ length: 20 }, (_, i) => i + 1);

// --- 유저 행동 유형
const ACTIONS = ['view', 'like', 'cart'];

export default function () {
    // 무작위 사용자 + 상품 + 행동
    const token = TOKENS[Math.floor(Math.random() * TOKENS.length)];
    const PRODUCT_IDS = Array.from({ length: 25 }, (_, i) => i + 7); // [7, 8, ..., 31]
    const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
    const action = ACTIONS[Math.floor(Math.random() * ACTIONS.length)];
    const quantity = Math.floor(Math.random() * 50) + 1;

    let res;

    // --- 행동별 API 라우팅
    if (action === 'view') {
        res = http.get(`http://munova-api:8080/api/product/${productId}`, {
            headers: { Authorization: token },
        });
    } else if (action === 'like') {
        const payload = JSON.stringify({
            productId: productId, // ✅ 명시적 key:value
        });
        res = http.post(`http://munova-api:8080/api/like`,payload, {
            headers: { Authorization: token, 'Content-Type': 'application/json' },
        });
    } else if (action === 'cart') {
        const payload = JSON.stringify({
            productDetailId: 1,
            quantity: quantity
        });
        res = http.post(`http://munova-api:8080/api/cart`,payload, {
            headers: {Authorization: token, 'Content-Type': 'application/json'},
        });
    }
    // } else if (action === 'purchase') {
    //     res = http.post(`http://munova-api:8080/api/order`, JSON.stringify({ productId }), {
    //         headers: { Authorization: token, 'Content-Type': 'application/json' },
    //     });
    // }

    // --- 응답 체크
    check(res, {
        [`${action} status 200`]: (r) => r.status === 200,
    });

    // --- 요청 간격 (사용자 행동 딜레이)
    sleep(0.3 + Math.random() * 0.7);
}