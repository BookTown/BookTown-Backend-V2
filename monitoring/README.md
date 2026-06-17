# BookTown Monitoring

Prometheus, Grafana, Loki, Promtail 운영 설정입니다.

## 접속

운영 서버에서는 모니터링 포트를 외부에 열지 않고 localhost에만 바인딩합니다.

```bash
ssh -i ~/.ssh/booktown-v2.pem \
  -L 3001:127.0.0.1:3001 \
  -L 9090:127.0.0.1:9090 \
  ubuntu@18.204.158.57
```

- Grafana: http://localhost:3001
- Prometheus: http://localhost:9090

## 필수 환경변수

`/opt/booktown/.env`에 Grafana 관리자 계정을 추가합니다.

```env
GRAFANA_ADMIN_USER=admin
GRAFANA_ADMIN_PASSWORD=change-me
GRAFANA_ROOT_URL=http://localhost:3001
```

## 수집 대상

- Prometheus: `backend:8080/api/v1/actuator/prometheus`
- Loki: Docker Compose 서비스 로그

## 보관 정책

- Prometheus: 3일 또는 1GB
- Loki: 72시간
