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

    //redis test
    const productId=Math.floor(Math.random()*5)/1;
    const res=http.post(`http://localhost:8080/recommend/logtest/${productId}`);

    // 응답 상태 체크 (성공률 확인용)
    check(res, { 'user recommend status 200': (r) => r.status === 200 });

    // 다음 요청 전 잠깐 대기 (부하 간격)
    sleep(1.0);
}