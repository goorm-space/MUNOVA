// 3개의 유저가 다양한 활동 추적
import http from 'k6/http';
import { sleep, check } from 'k6';

// --- 액세스 토큰 (예시용, 실제 테스트 시 여러 사용자 토큰 리스트로 교체 가능)
const TOKENS = [
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczEiLCJpYXQiOjE3NjI5MjU2NjUsImV4cCI6MTc2MzEwNTY2NX0.QF1lxYoJy6-yKGUthpVhotK0Xdkj9jFzgsTcKQpc1-_8EGmnfNUP4bI2TyiH8p9nQnE3nrDy3-DG0Km7_H8ilg',
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczIiLCJpYXQiOjE3NjI5MjU2NTYsImV4cCI6MTc2MzEwNTY1Nn0.i1NI24atPUwwJ-j0sgdbVTyQzRZ3B1AeRjo3z5VzQzSpG0o2uILscP8TvYeKQUx0GS_5FZaFizmyse3O4lAzYg', // 예시
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI3IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczMiLCJpYXQiOjE3NjI5MjU2NDQsImV4cCI6MTc2MzEwNTY0NH0.64cgGyE5l229ajlKx7b3Uo7ARIGbE-nX6k7B12tGyNwfOw0XbfdoXbJlQkSrR9vFfBhdoDFD7dTK8KEv9lceBQ'  // 예시
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
        res = http.get(`http://localhost:8080/api/product/${productId}`, {
            headers: { Authorization: token },
        });
    } else if (action === 'like') {
        const payload = JSON.stringify({
            productId: productId, // ✅ 명시적 key:value
        });
        res = http.post(`http://localhost:8080/api/like`,payload, {
            headers: { Authorization: token, 'Content-Type': 'application/json' },
        });
    } else if (action === 'cart') {
        const payload = JSON.stringify({
            productDetailId: 1,
            quantity: quantity
        });
        res = http.post(`http://localhost:8080/api/cart`,payload, {
            headers: {Authorization: token, 'Content-Type': 'application/json'},
        });
    }
    // } else if (action === 'purchase') {
    //     res = http.post(`http://localhost:8080/api/order`, JSON.stringify({ productId }), {
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