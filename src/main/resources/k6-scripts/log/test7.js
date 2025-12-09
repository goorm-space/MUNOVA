// 20개의 유저가 10개의 stream에 균등하게 분산되는지 테스트
import http from 'k6/http';
import {check, sleep} from 'k6';

// --- 부하 설정
export const options = {
    // VUS 20,000을 10초 동안 유지하는 부하 테스트
    // 단계적으로 증가하여 메모리/CPU 부하 분산
    // 한 번에 20,000 VUs를 생성하면 메모리 부족으로 터질 수 있음
    stages: [
        { duration: '2s', target: 5000 },   // 2초간 5,000 VUs로 증가 (램프업)
        { duration: '3s', target: 10000 },   // 3초간 10,000 VUs로 증가 (램프업)
        { duration: '10s', target: 20000 },  // 10초간 20,000 VUs 유지 (실제 부하 테스트)
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],    // 실패율 5% 이하 목표
        http_req_duration: ['p(95)<2000'], // 95% 요청 2초 이내
    },
    // 메모리 최적화: 연결 재사용 강화
    // k6는 기본적으로 연결 재사용을 하지만, 명시적으로 설정
};

// --- Member ID 범위 (1~20)
const MIN_MEMBER_ID = 1;
const MAX_MEMBER_ID = 20;
const PASSWORD = '1234';

// --- 상품 ID 풀
const PRODUCT_IDS = Array.from({length: 25}, (_, i) => i + 7); // [7, 8, ..., 31]

// --- 유저 행동 유형
const ACTIONS = ['view', 'like', 'cart'];

// 회원가입 및 로그인으로 토큰 생성
function generateToken(baseUrl, memberId) {
    const username = `ex${memberId}`;
    const deviceId = `device-${memberId}`;
    const address = `서울시 강남구 테스트동 ${memberId}번지`;

    // 1. 회원가입 시도
    const signUpPayload = JSON.stringify({
        username: username,
        password: PASSWORD,
        address: address
    });

    const signUpResponse = http.post(
        `${baseUrl}/auth/signup`,
        signUpPayload,
        {
            headers: {
                'Content-Type': 'application/json'
            }
        }
    );

    // 회원가입 실패 시 (이미 존재하는 경우) 무시
    if (signUpResponse.status !== 201 && signUpResponse.status !== 200 && signUpResponse.status !== 400) {
        console.log(`회원가입 실패: username=${username}, status=${signUpResponse.status}`);
    }

    // 2. 로그인 요청
    const signInPayload = JSON.stringify({
        username: username,
        password: PASSWORD
    });

    const signInResponse = http.post(
        `${baseUrl}/auth/signin`,
        signInPayload,
        {
            headers: {
                'Content-Type': 'application/json',
                'X-Device-Id': deviceId
            }
        }
    );

    if (signInResponse.status === 200) {
        const responseData = JSON.parse(signInResponse.body);
        if (responseData.data && responseData.data.accessToken) {
            return `Bearer ${responseData.data.accessToken}`;
        }
    }

    return null;
}

// setup 함수: 테스트 시작 전에 회원가입 및 토큰 생성
export function setup() {
    console.log('토큰 생성 시작...');
    const tokens = {};
    const baseUrl = 'http://munova-api:8080';

    // 1~20까지 토큰 생성
    for (let i = MIN_MEMBER_ID; i <= MAX_MEMBER_ID; i++) {
        const token = generateToken(baseUrl, i);
        if (token) {
            tokens[i] = token;
        } else {
            console.log(`토큰 생성 실패: memberId=${i}`);
        }

        // 진행률 표시
        if (i % 5 === 0) {
            console.log(`진행률: ${i}/${MAX_MEMBER_ID}`);
        }
    }

    console.log(`토큰 생성 완료: ${Object.keys(tokens).length}개`);
    return {tokens};
}

export default function (data) {
    // 1~20 사이의 랜덤 memberId 생성
    const memberId = Math.floor(Math.random() * (MAX_MEMBER_ID - MIN_MEMBER_ID + 1)) + MIN_MEMBER_ID;

    // setup에서 생성한 토큰 가져오기
    const token = data.tokens[memberId];

    if (!token) {
        console.log(`토큰 없음: memberId=${memberId}`);
        return;
    }

    const productId = PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
    const action = ACTIONS[Math.floor(Math.random() * ACTIONS.length)];
    const quantity = Math.floor(Math.random() * 50) + 1;

    let res;

    // --- 행동별 API 라우팅
    if (action === 'view') {
        res = http.get(`http://munova-api:8080/api/product/${productId}`, {
            headers: {Authorization: token},
        });
    } else if (action === 'like') {
        const payload = JSON.stringify({
            productId: productId, // ✅ 명시적 key:value
        });
        res = http.post(`http://munova-api:8080/api/like`, payload, {
            headers: {Authorization: token, 'Content-Type': 'application/json'},
        });
    } else if (action === 'cart') {
        const payload = JSON.stringify({
            productDetailId: 1,
            quantity: quantity
        });
        res = http.post(`http://munova-api:8080/api/cart`, payload, {
            headers: {Authorization: token, 'Content-Type': 'application/json'},
        });
    }

    // --- 응답 체크
    check(res, {
        [`${action} status 200`]: (r) => r.status === 200,
    });

    // --- 요청 간격 (사용자 행동 딜레이)
    sleep(1);

//    sleep(0.3 + Math.random() * 0.7);
}

