# ZIPDA API Gateway 아키텍처

[README로 돌아가기](../README.md)

## 1. Gateway 공통 요청 정책

SCG 서비스는 사용자 Client와 관리자 Client의 요청에 공통 정책을 적용한다.

### 1.1. CORS

두 Client는 서로 다른 주소에서 실행되지만 모두 SCG를 통해 Member·Property API를 호출한다. SCG는 두 프론트엔드 주소를 허용 Origin으로 관리하고, 브라우저의 교차 출처 API 접근 범위를 제한한다.

- **Origin 제한** → 사용자·관리자 프론트엔드 주소의 브라우저 API 호출 허용
- **HTTP 메서드 허용** → `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS` 요청 허용
- **인증 정보 전달** → Credential이 포함된 브라우저 요청 허용
- **응답 헤더 공개** → 프론트엔드에서 오류 추적용 `X-Trace-Id` 확인 가능

CORS는 허용되지 않은 웹사이트의 교차 출처 접근을 브라우저에서 제한하는 정책이다. 사용자 로그인 여부나 관리자 권한은 판단하지 않는다.

### 1.2. 요청 크기 제한

SCG 서비스는 모든 요청 본문을 최대 20MB로 제한한다. 파일별 형식과 크기는 각 하위 서비스에서 추가로 검증한다.

> **보안 적용 사항**  
> 허용된 프론트엔드 Origin의 브라우저 접근만 허용하고, 요청 본문을 20MB로 제한해 과도한 요청이 하위 서비스로 전달되는 것을 방지한다.

## 2. 서비스·문서 라우팅

SCG 서비스는 요청 경로에 따라 서비스 API와 OpenAPI 문서를 해당 하위 서비스로 전달한다.

### 2.1. 서비스 API 라우팅

서비스 라우팅은 `application.yaml`에 정의되어 있으며, 목적지 URI와 요청 경로 조건은 환경 변수로 주입한다. 로컬 환경은 실행 설정에서 지정한 `.env` 파일의 값을 사용한다.

```text
.env 또는 배포 환경 변수
    → application.yaml의 ${변수명} 참조
    → Spring Cloud Gateway Route 적용
```

| 환경 변수 | 용도 |
| --- | --- |
| `MEMBER_SERVICE_URI` | Member 서비스 목적지 |
| `MEMBER_SERVICE_PREDICATE` | Member 요청 경로 조건 |
| `PROPERTY_SERVICE_URI` | Property 서비스 목적지 |
| `PROPERTY_SERVICE_PREDICATE` | Property 요청 경로 조건 |

서비스 이름·목적지·경로 조건은 환경별로 주입하며 코드에 주소를 고정하지 않는다.

### 2.2. OpenAPI 문서 라우팅

Member 서비스와 Property 서비스는 각자의 OpenAPI JSON을 생성한다. SCG 서비스는 서비스별 문서 요청을 하위 서비스로 전달하고, `/docs`에서 통합 Swagger UI를 제공한다.

OpenAPI 문서 라우팅은 업무 API와 별도로 Member·Property의 문서 JSON을 전달하기 위한 개발 지원 경로이다.

```text
SCG /docs
  ├─ Member OpenAPI JSON → Member /api-docs
  └─ Property OpenAPI JSON → Property /api-docs
```

## 3. 추적·인증 정보 전달

SCG 서비스는 하위 서비스가 공통으로 사용할 요청 추적 정보와 인증 정보를 전달한다.

### 3.1. 요청 추적과 Trace ID 전달

SCG 서비스는 `X-Trace-Id`를 요청 추적 식별자로 사용한다. 여러 서비스의 로그를 동일한 요청 단위로 연결하기 위한 기준이다.

- 값이 없거나 비어 있으면 UUID 생성
- 허용 문자와 1~100자 길이 검증
- 올바르지 않은 값은 새 UUID로 교체
- 하위 서비스 요청과 클라이언트 응답에 동일한 값 전달

현재 `TraceIdFilter`가 이 기능을 담당한다. 인증 오류 응답도 추적할 수 있도록 `AuthFilter`보다 먼저 실행된다.

### 3.2. JWT 인증과 사용자 정보 전달

SCG 서비스는 JWT를 검증하고 사용자 정보를 내부 헤더로 변환해 하위 서비스에 전달한다.

- JWT가 있으면 서명과 만료 검증
- 정상 JWT는 X-User-Id, X-User-Type, X-User-Role로 변환해 전달
- 원본 Authorization 헤더는 하위 서비스에 전달하지 않음
- JWT가 없으면 사용자 정보를 생성하지 않고 요청 전달
- 잘못된 JWT는 하위 서비스 도달 전에 401/E04로 차단

현재 `AuthFilter`가 이 기능을 담당한다.

## 4. SCG 오류 응답

SCG에서 발생한 인증·라우팅·시스템 오류는 동일한 응답 구조로 반환한다.

```json
{
  "code": "E04",
  "message": "SCG_INVALID_TOKEN_ERROR",
  "data": null,
  "traceId": "request-trace-id"
}
```

| 상황 | HTTP 상태 | 코드 |
| --- | ---: | --- |
| 유효하지 않은 JWT | 401 | `E04` |
| SCG 경로 없음 | 404 | `E50` |
| SCG 처리 오류 | 500 | `E99` |

오류 응답은 `code`, `message`, `data`, `traceId` 구조로 반환한다.

## 5. Gateway와 도메인 서비스 책임 경계

- SCG 서비스는 외부 요청 진입점과 Gateway 공통 정책을 담당한다.
- Member·Property는 자신의 API와 업무 규칙을 담당한다.
- SCG 서비스가 JWT를 검증해도 최종 업무 권한은 하위 서비스가 판단한다.
- Property → Member 내부 권한 조회는 SCG 서비스의 라우팅 대상이 아니다.
