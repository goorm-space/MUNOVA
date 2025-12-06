// 상품 조회 (GET /product) 부하 테스트 스크립트 (랜덤 파라미터 강화)
import http from 'k6/http';
import { sleep, check } from 'k6';

// --- 액세스 토큰 (예시용)
const TOKENS = [
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI1IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczEiLCJpYXQiOjE3NjI5MjU2NjUsImV4cCI6MTc2MzEwNTY2NX0.QF1lxYoJy6-yKGUthpVhotK0Xdkj9jFzgsTcKQpc1-_8EGmnfNUP4bI2TyiH8p9nQnE3nrDy3-DG0Km7_H8ilg',
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI2IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczIiLCJpYXQiOjE3NjI5MjU2NTYsImV4cCI6MTc2MzEwNTY1Nn0.i1NI24atPUwwJ-j0sgdbVTyQzRZ3B1AeRjo3z5VzQzSpG0o2uILscP8TvYeKQUx0GS_5FZaFizmyse3O4lAzYg',
    'Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiI3IiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJyZWRpczMiLCJpYXQiOjE3NjI5MjU2NDQsImV4cCI6MTc2MzEwNTY0NH0.64cgGyE5l229ajlKx7b3Uo7ARIGbE-nX6k7B12tGyNwfOw0XbfdoXbJlQkSrR9vFfBhdoDFD7dTK8KEv9lceBQ'
];

// --- 부하 설정
export const options = {
    vus: 100,          // 동시 접속자 수
    duration: '30s',   // 테스트 지속 시간
    thresholds: {
        http_req_failed: ['rate<0.05'],    // 실패율 5% 이하 목표
        http_req_duration: ['p(95)<2000'], // 95% 요청 2초 이내
    },
};

export default function () {
    const token = TOKENS[Math.floor(Math.random() * TOKENS.length)];

    // 1. Sort Flag (항상 포함)
    const sortFlags = ['CREATED_AT', 'SALES_COUNT', 'LIKE_COUNT', 'VIEW_COUNT'];
    const randomSort = sortFlags[Math.floor(Math.random() * sortFlags.length)];

    // 2. Keyword (70% 확률로 포함)
    const keywords = ['신발', 'promotion', '셔츠', '바지', '모자', '가방', '나이키', '아디다스', '여름', '겨울'];
    let randomKeyword = null;
    if (Math.random() > 0.3) {
        randomKeyword = keywords[Math.floor(Math.random() * keywords.length)];
    }

    // 3. Category ID (50% 확률로 포함)
    let randomCategory = null;
    if (Math.random() > 0.5) {
        randomCategory = Math.floor(Math.random() * 10) + 1; // 1~10
    }

    // 4. Option IDs (30% 확률로 포함, 여러 개 선택 가능)
    const availableOptionIds = [1, 2, 9, 10, 11, 18, 20, 21];
    let selectedOptionIds = null;
    if (Math.random() > 0.7) {
        // 1개에서 3개 사이의 옵션 랜덤 선택
        const count = Math.floor(Math.random() * 3) + 1;
        // 배열 섞어서 앞에서부터 count개 가져오기
        const shuffled = availableOptionIds.sort(() => 0.5 - Math.random());
        selectedOptionIds = shuffled.slice(0, count);
    }

    // 쿼리 파라미터 객체 구성
    const params = {
        sortFlag: randomSort,
        page: 0,
        size: 20
    };

    // 값이 있는 경우에만 파라미터 추가
    if (randomCategory !== null) {
        params.categoryId = randomCategory;
    }
    if (randomKeyword !== null) {
        params.keyword = randomKeyword;
    }
    if (selectedOptionIds !== null) {
        // 배열을 콤마로 구분된 문자열로 변환 (예: "1,2,10")
        params.optionIds = selectedOptionIds.join(',');
    }

    // 쿼리 스트링 변환
    const queryString = Object.keys(params)
        .map(key => `${key}=${encodeURIComponent(params[key])}`)
        .join('&');

    // --- API 요청
    const res = http.get(`http://localhost:8080/product?${queryString}`, {
        headers: {
            Authorization: token,
            'Content-Type': 'application/json'
        },
    });

    // --- 응답 체크
    check(res, {
        'status is 200': (r) => r.status === 200,
        'response has content': (r) => r.body && r.body.length > 0,
    });

    // --- 요청 간격
    sleep(0.3 + Math.random() * 0.7);
}
