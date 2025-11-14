import http from 'k6/http';
import { sleep, check } from 'k6';
const bearId='eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIxIiwiYXV0aG9yaXRpZXMiOiJVU0VSIiwidXNlcm5hbWUiOiJ1c2VyIiwiaWF0IjoxNzYyODQwNTM1LCJleHAiOjE3NjI4NDIzMzV9.L_j8dsqhZuacn5C5SGEeyEEI5_nYrwnuHDv0I91w13Bu1JU_HLvq7ebMkM--nZX3_X7yF9qVNM_rM0k0Wo1G0Q';
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
    const productId=1;
    const res = http.get(`http://localhost:8080/api/product/${productId}`, {
        headers: { Authorization: TOKEN },
    });

    // 응답 상태 체크 (성공률 확인용)
    check(res, { 'user recommend status 200': (r) => r.status === 200 });

    // 다음 요청 전 잠깐 대기 (부하 간격)
    sleep(1.0);
}