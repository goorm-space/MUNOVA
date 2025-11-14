
import http from 'k6/http';
import { sleep } from 'k6';

// 🚨🚨🚨 여기가 핵심 🚨🚨🚨
export const options = {
    scenarios: {
        contacts: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
        },
    },

    // k6가 '모든' 기본 지표를 InfluxDB로 쏘도록 설정
    ext: {
        loadimpact: {
            influxdb: {
                // (중요!) 'influxdb'는 docker-compose의 서비스 이름
                url: 'http://influxdb:8086',
                database: 'k6', // docker-compose에서 만든 'k6' DB
            }
        }
    }
};
// 🚨🚨🚨 여기까지 🚨🚨🚨


export default function () {
    // 이제 k6가 알아서 '결과'를 쏘므로,
    // 'default' 함수는 순수하게 '테스트'만 하면 됩니다.
    const res = http.get('https://test.k6.io');
    sleep(1);


}