import http from 'k6/http';
import { sleep, check } from 'k6';
const bearId='eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJ1c2VyIiwiaWF0IjoxNzYyMzk0OTk2LCJleHAiOjE3NjIzOTY3OTZ9.bhsXG6MNY29zJuQyKpFnjfdImDU7W8Z48_HJUpRU1VH1c0qkH_HYOzSCTYe0-QQFWH59iIRi_67njX-7qYRizQ';
const TOKEN='Bearer '+bearId
// 테스트 옵션
export const options = {
    vus: 10000,          // dau
    duration: '10s',  // 부하 시간
};

// 테스트 시나리오
export default function () {
    const headers= {Authorization: TOKEN};

    // 1) 회원 맞춤 추천 API 호출
    const res1 = http.put('http://localhost:8080/api/recommend/user/123',null,{ headers });

    // 2) 유사 상품 추천 API 호출
    const res2 = http.put('http://localhost:8080/recommend/123');


    // 응답 상태 체크 (성공률 확인용)
    check(res1, { 'user recommend status 200': (r) => r.status === 200 });
    check(res2, { 'similar recommend status 200': (r) => r.status === 200 });

    // 다음 요청 전 잠깐 대기 (부하 간격)
    sleep(0.2);
}