# JWT 인증과 Gateway 신뢰 경계

[README로 돌아가기](../README.md)

## 1. 인증 책임 경계

ZIPDA MSA에서 SCG는 외부 Client의 JWT를 검증하고 내부 사용자 정보로 변환하는 Gateway 신뢰 경계이다. SCG는 사용자 인증을 담당하고, Member·Property 서비스는 전달받은 사용자 정보로 기능별 접근 권한을 판단한다.

```text
Client / Admin Client
  └─ Authorization: Bearer <access-token>
        ↓
SCG
  └─ JWT 검증·사용자 정보 변환
        ↓
Member / Property
  └─ 기능별 접근 권한 확인
```

토큰이 없는 요청은 그대로 전달되며, 공개·보호 API의 최종 접근 허용 여부는 하위 서비스의 보안 규칙이 결정한다.

## 2. JWT 인증 처리

```text
Authorization: Bearer <access-token>
  → Bearer Token 추출
  → 서명·만료·형식 검증
  → Claim과 역할 구조 확인
  → 내부 사용자 정보 생성
  → 하위 서비스 전달
```

- 정상 JWT → 사용자 정보로 변환해 하위 서비스에 전달
- JWT 없음 → 사용자 정보를 생성하지 않고 요청 전달
- 만료·변조·형식 오류 → 하위 서비스 전달 전에 `401/E04`로 차단

## 3. 사용자 정보 변환과 전달

### 3.1. Claim과 내부 헤더

| JWT Claim | 내부 헤더 | 용도 |
| --- | --- | --- |
| `sub` | `X-User-Id` | 인증 주체 식별자 |
| `type` | `X-User-Type` | `MEMBER`·`ADMIN` 구분 |
| `role` | `X-User-Role` | 회원 단일 역할 |
| `roles` | `X-User-Role` | 관리자 다중 역할을 쉼표로 연결 |

유효한 JWT를 처리할 때 SCG 서비스는 원본 `Authorization` 헤더를 제거하고 JWT Claim에서 생성한 내부 사용자 정보를 전달한다.

> **보안 적용 사항**  
> 검증이 끝난 원본 Access Token을 하위 서비스에 전달하지 않아 토큰 노출 범위를 줄인다.

### 3.2. 역할 변환

- 회원 → `role` 문자열 하나를 전달
- 관리자 → `roles` 배열을 검증하고 중복 제거 후 쉼표로 연결
- 지원하지 않는 `type`, 빈 역할, 잘못된 역할 구조 → `401/E04`로 처리

## 4. JWT 서명 키 관리

SCG의 `JWT_SECRET`은 Member 서비스가 토큰 서명에 사용하는 키와 동일해야 한다. SCG는 Base64로 디코딩한 HMAC 키로 JWT 서명을 검증한다.

- JJWT HMAC 요구 길이를 만족하는 Base64 값 사용
- 로컬 환경은 실행 설정에서 지정한 비밀 파일로 주입
- 배포 환경은 Kubernetes Secret 등 배포 Secret으로 주입
- 실제 값은 문서·Git·로그에 기록하지 않음
