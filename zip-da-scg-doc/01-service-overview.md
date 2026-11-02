# SCG 서비스 개요

[README로 돌아가기](../README.md)

## 1. 서비스 소개

`zip-da-scg`는 Spring Cloud Gateway로 구현한 서비스로, ZIPDA에서 Member·Property로 향하는 외부 API 요청의 진입점 역할을 담당한다. 공통 인증·추적 정책을 적용한 뒤 요청 경로에 따라 Member 또는 Property 서비스로 전달한다.

## 2. 핵심 역할

- 환경 변수 기반 Member·Property 서비스 라우팅
- JWT 검증과 하위 서비스용 인증 정보 전달
- Trace ID, CORS, 요청 제한과 오류 응답 공통 처리
- Member·Property 문서를 제공하는 통합 Swagger UI

## 3. 서비스 책임 범위

| 구분 | 담당 서비스 |
| --- | --- |
| 외부 요청 진입·라우팅 | SCG |
| JWT 검증과 인증 정보 전달 | SCG |
| 회원·관리자·권한 기준 데이터 | Member |
| 매물 데이터와 매물 업무 | Property |

SCG 서비스는 업무 데이터를 저장하지 않으며 Member·Property 서비스의 업무 권한을 대신 결정하지 않는다. 하위 서비스는 전달받은 인증 정보와 각 서비스의 정책을 함께 적용한다.
