# 개발 환경 세팅

[README로 돌아가기](../README.md)

## 1. 필수 도구

| 도구 | 기준 |
| --- | --- |
| JDK | 21 |
| Gradle | 저장소의 Wrapper 9.5.1 사용 |
| 하위 서비스 | 통합 검증 시 Member·Property 실행 |

Java와 Gradle Wrapper가 정상 실행되는지 확인한다.

```bash
java -version
./gradlew --version
```

## 2. 환경 변수

### 2.1. 공통 설정

| 변수 | 용도 |
| --- | --- |
| `APP_PORT` | SCG 실행 포트 |
| `JWT_SECRET` | JWT 검증용 Base64 HMAC 키 |
| `CORS_ALLOW_ORIGIN_CLIENT` | 사용자 프론트엔드 Origin |
| `CORS_ALLOW_ORIGIN_ADMIN_CLIENT` | 관리자 프론트엔드 Origin |

### 2.2. 서비스 라우팅

| 변수 | 용도 |
| --- | --- |
| `MEMBER_SERVICE_NAME` | Member 서비스와 문서 표시 이름 |
| `MEMBER_SERVICE_URI` | Member 목적지 URI |
| `MEMBER_SERVICE_PREDICATE` | Member 요청 Path 조건 |
| `MEMBER_SERVICE_OPEN_API_PATH` | Member OpenAPI JSON 경로 |
| `PROPERTY_SERVICE_NAME` | Property 서비스와 문서 표시 이름 |
| `PROPERTY_SERVICE_URI` | Property 목적지 URI |
| `PROPERTY_SERVICE_PREDICATE` | Property 요청 Path 조건 |
| `PROPERTY_SERVICE_OPEN_API_PATH` | Property OpenAPI JSON 경로 |

### 2.3. 로컬 개발 환경

- 로컬 실행에 필요한 환경 변수는 `.env` 파일에 설정한다.
- `.env` 파일을 설정하고, Spring Boot 애플리케이션 또는 Gradle `bootRun`을 실행할 때 환경 변수가 적용된다.
  - IntelliJ IDEA 실행 설정 예시: Run/Debug Configuration 환경 변수 파일(`.env`)의 경로 지정
  - Git Bash 명령에서 경로 지정 예시: `source` 명령으로 환경 변수 파일을 불러와 설정

  ```bash
  set -a
  source /path/to/.env
  set +a
  ./gradlew bootRun
  ```

- 실제 비밀값은 로컬 `.env`에만 보관하고 문서·Git·로그에는 기록하지 않는다.
- `JWT_SECRET`은 JJWT HMAC 요구 길이를 만족하는 Base64 값이어야 한다.
- `application.yaml`은 `${변수명}` 형식으로 주입된 환경 변수를 참조한다.

### 2.4. 운영 환경 참고

- 운영에서는 중요정보를 Kubernetes `Secret`, 일반 설정을 `ConfigMap`으로 관리하고, `Deployment`가 이를 환경 변수로 주입한다.
- 운영 `Deployment`에서 `SPRING_PROFILES_ACTIVE=prod`를 설정하면 `application-prod.yaml` 설정이 적용된다.

```text
Kubernetes Secret / ConfigMap
    → Deployment 환경 변수 주입
    → application.yaml의 ${변수명} 참조
    → Spring Boot 설정 적용
```

## 3. 검증

```bash
# 실행 가능한 JAR 생성
./gradlew clean bootJar
```

정상 기준은 다음과 같다.

- JAR 생성: BUILD SUCCESSFUL이 표시되고 build/libs에 실행 가능한 JAR 파일이 생성된다.

## 4. SCG·하위 서비스 연동 체크리스트

SCG·Member·Property 서비스를 함께 실행한 뒤 필요한 항목을 확인한다.

- [ ] **Member 요청 전달**
  - 확인 방법: SCG의 `/api/member/**` 경로 호출
  - 정상 기준: SCG를 통해 Member 서비스 응답 반환

- [ ] **Property 요청 전달**
  - 확인 방법: SCG의 `/api/property/**` 경로 호출
  - 정상 기준: SCG를 통해 Property 서비스 응답 반환

- [ ] **유효한 JWT 인증**
  - 확인 방법: 접근 권한이 있는 유효한 JWT로 보호 API 호출
  - 정상 기준: 인증 정보가 하위 서비스에 전달되고 정상 응답 반환

- [ ] **유효하지 않은 JWT 차단**
  - 확인 방법: 만료되거나 변조된 JWT로 API 호출
  - 정상 기준: SCG에서 `401/E04` 응답 반환

- [ ] **Trace ID 전파**
  - 확인 방법: `X-Trace-Id`를 포함해 API 호출
  - 정상 기준: 응답 헤더와 하위 서비스 로그에서 동일한 값 확인

- [ ] **허용 Origin CORS 응답**
  - 확인 방법: 설정한 프론트엔드 Origin으로 `OPTIONS` 요청
  - 정상 기준: 응답의 `Access-Control-Allow-Origin`에서 요청 Origin 확인

- [ ] **요청 크기 제한**
  - 확인 방법: 20MB를 초과하는 요청 전송
  - 정상 기준: 하위 서비스 전달 전에 SCG에서 요청 거부

- [ ] **통합 API 문서 로딩**
  - 확인 방법: `http://localhost:8080/docs` 접속
  - 정상 기준: Member·Property API 문서 모두 로딩

## 5. 문제 해결 체크리스트

문제가 발생하면 증상에 해당하는 설정을 확인한다.

- [ ] **SCG 실행 시 JWT 초기화 실패**
  - 확인 사항: `JWT_SECRET`의 Base64 형식과 HMAC 키 길이

- [ ] **하위 서비스 연결 실패**
  - 확인 사항: Member·Property 목적지 URI, 실행 상태와 포트

- [ ] **API 요청에서 404 응답**
  - 확인 사항: 서비스 Predicate와 실제 API 요청 경로

- [ ] **통합 API 문서 로딩 실패**
  - 확인 사항: 하위 서비스 실행 상태, 서비스 이름, OpenAPI 경로와 Rewrite 설정

- [ ] **브라우저 CORS 오류**
  - 확인 사항: 요청 Origin과 허용 Origin 환경 변수의 프로토콜·호스트·포트
