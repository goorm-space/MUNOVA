// 다양한 로그 케이스를 테스트하기 위한 확장된 시나리오
// PRODUCT, ORDER, COUPON, CHAT 등 다양한 StreamType에 대한 로그 생성
import http from 'k6/http';
import { sleep, check } from 'k6';

// --- 액세스 토큰 (예시용, 실제 테스트 시 여러 사용자 토큰 리스트로 교체 가능)
const TOKENS = [
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczEiLCJpYXQiOjE3NjMzMzkzMTYsImV4cCI6MTc2MzUxOTMxNn0.oL_Sj2c-jBoAB7T6wIcC8c0yEd_6HQZx49OqG6UXKoMVX4w28yHK_SgbeyHh5EhmAb00exsjUc_RhqcZxWh5Vw',
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczIiLCJpYXQiOjE3NjMzMzkzMzEsImV4cCI6MTc2MzUxOTMzMX0.j4AWvJ0c7JB35fbQLkHwchl-NhsiFYxFoFtyocTaFzKsWkjrcyoa10HdwnBVF6rvB0rM8ZKce8HyxKf2Fl0ueA', // 예시
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI3IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczMiLCJpYXQiOjE3NjMzMzkzNDEsImV4cCI6MTc2MzUxOTM0MX0.nYwZtlU4HxteUzuQT8jL4JF7ubBC1cuZQP_1qRNmv_mdx3gF3VyCQgBG7hJQc7qyW94MTO6UnNTPFSbK-XAjBg'  // 예시
];

// --- 부하 설정
export const options = {
    vus: 10000,        // 가상의 사용자 (동시 접속자)
    duration: '5s',   // 테스트 지속 시간
    thresholds: {
        http_req_failed: ['rate<0.05'],    // 실패율 5% 이하 목표
        http_req_duration: ['p(95)<2000'], // 95% 요청 2초 이내
    },
};

// --- 상품 ID 풀 (예: 1~1000)
const PRODUCT_IDS = Array.from({ length: 20 }, (_, i) => i + 1);

// --- 유저 행동 유형
// PRODUCT: view, like, cart
// ORDER: order_create, order_list, order_detail
// COUPON: coupon_list, coupon_issue
// CHAT: chat_create_one_to_one, chat_list_one_to_one, chat_group_search, chat_group_detail
const ACTIONS = [
    'view',                      // 상품 상세 조회
    'like',                      // 상품 좋아요
    'cart',                      // 장바구니 추가
    'order_create',              // 주문 생성
    'order_list',                // 주문 목록 조회
    'order_detail',              // 주문 상세 조회
    'coupon_list',               // 쿠폰 목록 조회
    'coupon_issue',              // 쿠폰 발급
    'chat_create_one_to_one',    // 1:1 채팅방 생성
    'chat_list_one_to_one',      // 1:1 채팅방 목록 조회
    'chat_group_search',         // 그룹 채팅방 검색
    'chat_group_detail'           // 그룹 채팅방 상세 조회
];

export default function () {
    // 무작위 사용자 + 상품 + 행동
    const token = TOKENS[Math.floor(Math.random() * TOKENS.length)];
    const PRODUCT_IDS = Array.from({ length: 25 }, (_, i) => i + 7); // [7, 8, ..., 31]
    const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
    const productDetailId = Math.floor(Math.random() * 10) + 1;
    const action = ACTIONS[Math.floor(Math.random() * ACTIONS.length)];
    const quantity = Math.floor(Math.random() * 50) + 1;
    const page = Math.floor(Math.random() * 3); // 0~2 페이지

    let res;

    // --- 행동별 API 라우팅
    if (action === 'view') {
        // 상품 상세 조회
        res = http.get(`http://munova-api:8080/api/product/${productId}`, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'like') {
        // 상품 좋아요
        // ProductLikeRequestDto: productId (Long)
        const payload = JSON.stringify({
            productId: productId
        });
        res = http.post(`http://munova-api:8080/api/like`, payload, {
            headers: { 
                'Authorization': token, 
                'Content-Type': 'application/json' 
            },
        });
    } else if (action === 'cart') {
        // 장바구니 추가
        // AddCartItemRequestDto: productDetailId (Long), quantity (int)
        const payload = JSON.stringify({
            productDetailId: productDetailId,
            quantity: quantity
        });
        res = http.post(`http://munova-api:8080/api/cart`, payload, {
            headers: { 
                'Authorization': token, 
                'Content-Type': 'application/json' 
            },
        });
    } else if (action === 'order_create') {
        // 주문 생성
        // CreateOrderRequest: orderCouponId (Long), userRequest (String), clientCalculatedAmount (Long), orderItems (List<OrderItemRequest>)
        // OrderItemRequest: productDetailId (Long), quantity (Integer)
        const orderItems = [
            {
                productDetailId: productDetailId,
                quantity: quantity
            }
        ];
        const payload = JSON.stringify({
            orderCouponId: null,
            userRequest: '테스트 주문 요청',
            clientCalculatedAmount: quantity * 10000,
            orderItems: orderItems
        });
        res = http.post(`http://munova-api:8080/api/orders`, payload, {
            headers: { 
                'Authorization': token, 
                'Content-Type': 'application/json' 
            },
        });
    } else if (action === 'order_list') {
        // 주문 목록 조회
        res = http.get(`http://munova-api:8080/api/orders?page=${page}`, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'order_detail') {
        // 주문 상세 조회 (임의의 orderId 사용, 실제로는 존재하지 않을 수 있음)
        const orderId = Math.floor(Math.random() * 10) + 1;
        res = http.get(`http://munova-api:8080/api/orders/${orderId}`, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'coupon_list') {
        // 쿠폰 목록 조회
        res = http.get(`http://munova-api:8080/api/coupon?page=${page}`, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'coupon_issue') {
        // 쿠폰 발급 (임의의 couponDetailId 사용)
        // POST /api/coupon/{couponDetailId} - body 없음
        const couponDetailId = Math.floor(Math.random() * 10) + 1;
        res = http.post(`http://munova-api:8080/api/coupon/${couponDetailId}`, null, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'chat_create_one_to_one') {
        // 1:1 채팅방 생성
        // POST /api/chat/one-to-one/{productId} - body 없음
        res = http.post(`http://munova-api:8080/api/chat/one-to-one/${productId}`, null, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'chat_list_one_to_one') {
        // 1:1 채팅방 목록 조회
        res = http.get(`http://munova-api:8080/api/chat/one-to-one`, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'chat_group_search') {
        // 그룹 채팅방 검색
        // GET /api/chat/group/search?keyword={keyword}&tagIds={tagIds}&isMine={isMine}
        const keyword = ['운동', '패션', '전자제품', '음식', null][Math.floor(Math.random() * 5)];
        const tagIds = Math.random() > 0.5 ? [1, 2] : null;
        const isMine = Math.random() > 0.5;
        let url = `http://munova-api:8080/api/chat/group/search?isMine=${isMine}`;
        if (keyword) {
            url += `&keyword=${encodeURIComponent(keyword)}`;
        }
        if (tagIds) {
            url += `&tagIds=${tagIds.join(',')}`;
        }
        res = http.get(url, {
            headers: { 'Authorization': token },
        });
    } else if (action === 'chat_group_detail') {
        // 그룹 채팅방 상세 조회 (임의의 chatId 사용)
        const chatId = Math.floor(Math.random() * 10) + 1;
        res = http.get(`http://munova-api:8080/api/chat/group/${chatId}`, {
            headers: { 'Authorization': token },
        });
    }

    // --- 응답 체크 (404나 다른 에러도 정상적인 테스트 케이스로 간주)
    check(res, {
        [`${action} status ok`]: (r) => r.status >= 200 && r.status < 500,
    });

    // --- 요청 간격 (사용자 행동 딜레이)
    sleep(0.3 + Math.random() * 0.7);
}

