# 🐾 따숨 (Ddasoom)

> **따뜻한 숨결** — 유기동물에게 따뜻한 손길을 이어주는 임시보호 & 커뮤니티 플랫폼

유기동물 정보를 확인하고, 임시보호를 신청하고, 반려동물 정보를 나누는 웹 서비스입니다.

---

## 📌 주요 기능

| # | 기능 | 설명 |
|---|------|------|
| 1 | **유기동물 정보 조회** | 공공데이터 포털 API(유기동물 정보)를 DB에 캐싱(미러링)하여 목록/상세 페이지 제공, 검색 및 좋아요 기능 |
| 2 | **임시보호 신청** | 유기동물 임시보호 신청, 받은 신청 관리 (코멘트 작성, 상태 변경) |
| 3 | **펫 커뮤니티** | 반려동물 정보 교환 게시판, 입양 후기 작성 게시판 |
| 4 | **회원 시스템** | 이메일 인증 기반 일반 회원가입, SNS 로그인(구글/카카오/네이버), JWT 인증 |
| 5 | **관리자 페이지** | 임시보호 신청 관리, 회원 관리, 게시글/댓글 관리, 공지사항, FAQ, 1:1 문의 |

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | Java 17 |
| Framework | Spring Boot 3.x, Spring Security, Spring Data JPA |
| Auth | JWT, OAuth2 (Google / Kakao / Naver), SMTP 이메일 인증 |
| Database | MySQL (RDB), Redis (인증 코드 / 토큰 저장) |
| Storage | MinIO (이미지 파일 저장) |
| Infra | Docker |

---

## 🚀 로컬 실행 방법

### 1. 사전 준비

- Java 17, Docker 설치
- Redis 컨테이너 실행

```bash
docker run -d -p 6379:6379 --name ddasoom-redis redis
```

### 2. application.yml 설정

`application.yml-example`을 복사하여 `application.yml`을 만들고 아래 항목을 채웁니다.

```yaml
spring:
  mail:
    username: # SMTP 발신 계정 (Gmail 사용 시 앱 비밀번호 발급 필요)
    password: # SMTP 앱 비밀번호

app:
  cors:
    allowed-origins: http://localhost:5173

ddasoom:
  service-url: http://localhost:5173   # 이메일 내 버튼이 이동할 프론트 주소

minio:
  endpoint: # MinIO 엔드포인트
  access-key: # MinIO Access Key
  secret-key: # MinIO Secret Key
```

> ⚠️ `application.yml`은 절대 커밋하지 않습니다. (`.gitignore` 등록 확인)

### 3. 실행

```bash
./gradlew bootRun
```

### 4. 회원가입 동작 확인 (Postman)

```
POST /api/auth/email/send    → 인증 코드 발송 (메일 수신 확인)
POST /api/auth/email/verify  → 인증 코드 검증
POST /api/auth/signup        → 회원가입 (201 Created)
```

---

## 📂 프로젝트 구조

```
com.paw.ddasoom
├── common          # 공통 모듈 (설정, 예외, 응답 DTO, 시큐리티 인프라)
│   ├── config      # RedisConfig, PasswordEncoderConfig, MinioConfig, WebConfig
│   ├── dto         # ApiResponse (공통 응답 규격)
│   ├── exception   # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── security    # SecurityConfig, SecurityConstants, CorsProperties
│   └── util        # BaseTimeEntity, SecurityUtil
├── auth            # 인증 도메인 (회원가입, 로그인, 이메일 인증, 토큰)
├── member          # 회원 도메인 (회원 정보 관리)
└── ...             # animal, foster, community 등 도메인 추가 예정
```

> 도메인 경계 기준 등 상세 규칙은 아래 문서를 참고하세요.

---

## 📚 팀 문서

| 문서 | 내용 |
|------|------|
| [백엔드 코드 컨벤션](./docs/CONVENTIONS.md) | 패키지 구조, 네이밍, 예외 처리, API 공통 규격 등 코드 작성 규칙 |
| [Security Flow](docs/SECURITY_FLOW.md) | 인증/인가 흐름, 컨트롤러에서 로그인 회원 정보 꺼내는 방법 |

---

## 👥 팀

| 역할 | 담당 |
|------|-----|
| 유기동물 API 연동 / 조회 | 김종식 |
| 임시보호 신청 | 김경우 |
| 커뮤니티 / 입양 후기 | 유창호 |
| **회원 / 인증 (Auth, Member, Security)** | 구지훈 |
| 관리자 페이지 | 이서진 |
