# 🐾 따숨 (Ddasoom) — Backend

> **따뜻한 숨결** — 유기동물에게 따뜻한 손길을 이어주는 임시보호 & 커뮤니티 플랫폼

유기동물 정보를 확인하고, 임시보호를 신청하고, 반려동물 정보를 나누는 웹 서비스의 백엔드입니다.

---

## 📌 주요 기능

| # | 기능 | 설명 |
|---|------|------|
| 1 | **유기동물 정보 조회** | 공공데이터 포털 API(유기동물 정보)를 DB에 캐싱(미러링)하여 목록/상세 제공, 검색·좋아요 |
| 2 | **임시보호 신청** | 유기동물 임시보호 신청, 받은 신청 관리 (심사 코멘트, 상태 전이: 대기→승인/반려→연장→종료) |
| 3 | **펫 커뮤니티** | 강아지/고양이 정보 게시판, 입양 후기 게시판 (이미지 하이브리드 업로드) |
| 4 | **회원 시스템** | 이메일 인증 기반 일반 회원가입, SNS 로그인(구글/카카오/네이버), JWT 인증 |
| 5 | **신고 / 제재** | 게시글·댓글·회원 신고 접수, 관리자 승인 시 대상 숨김(회원 HIDDEN) 처리 |
| 6 | **관리자 페이지** | 대시보드·통계, 회원 관리(강제탈퇴/복구/상태변경), 신고 관리, 공지·FAQ·1:1 문의 |

---

## 🛠️ 기술 스택

| 분류 | 기술 |
|------|------|
| Language | **Java 21** |
| Framework | **Spring Boot 3.5.x**, Spring Security, Spring Data JPA |
| Query | **QueryDSL** (통계/집계 등 동적·복합 쿼리) |
| Auth | JWT (AT/RT + 로테이션), OAuth2 (Google / Kakao / Naver), SMTP 이메일 인증 |
| Database | MySQL (RDB), **Flyway** (마이그레이션), Redis (인증 코드 / 토큰 / 세션 상태) |
| Storage | MinIO (이미지 파일 저장 — 공개/비공개 버킷 분리) |
| Docs | **springdoc-openapi (Swagger UI)** |
| Infra | Docker |

---

## 🚀 로컬 실행 방법

### 1. 사전 준비

- **Java 21**, Docker 설치
- Redis / MinIO 컨테이너 실행

```bash
# Redis
docker run -d -p 6379:6379 --name ddasoom-redis redis

# MinIO (이미지 저장 — 미기동 시 서버는 뜨지만 이미지 기능만 비활성)
docker run -d -p 9000:9000 -p 9001:9001 --name ddasoom-minio \
  -e MINIO_ROOT_USER=minioadmin -e MINIO_ROOT_PASSWORD=minioadmin \
  minio/minio server /data --console-address ":9001"
```

### 2. 환경변수 설정 (⚠️ application.yml에 시크릿 하드코딩 금지)

시크릿은 `application.yml`에 직접 쓰지 않고 **환경변수(`DDASOOM_*`)로 주입**합니다. yml에는 `${DDASOOM_...}` 플레이스홀더만 존재하며, 미등록 시 **기동 시점에 실패(fail-fast)** 합니다.

시크릿 환경변수 10종 (yml의 ${DDASOOM_*} 플레이스홀더 — 미등록 시 기동 실패):

DDASOOM_MAIL_USERNAME         # SMTP 발신 구글 이메일
DDASOOM_MAIL_PASSWORD         # 구글 16자리 앱 비밀번호 (공백 없이)
DDASOOM_GOOGLE_CLIENT_ID      # 구글 OAuth client-id
DDASOOM_GOOGLE_CLIENT_SECRET  # 구글 OAuth client-secret
DDASOOM_KAKAO_CLIENT_ID       # 카카오 OAuth client-id
DDASOOM_KAKAO_CLIENT_SECRET   # 카카오 OAuth client-secret
DDASOOM_NAVER_CLIENT_ID       # 네이버 OAuth client-id
DDASOOM_NAVER_CLIENT_SECRET   # 네이버 OAuth client-secret
DDASOOM_PUBLIC_API_KEY        # 공공데이터포털(유기동물) 서비스 키
DDASOOM_JWT_SECRET            # JWT HS512 서명 키 (BASE64)

> JWT 유효기간(access/refresh-token-validity), CORS 허용 오리진, service-url(프론트 주소)은
> 시크릿이 아니라 yml에 평문으로 관리합니다(로컬 표준값). MySQL/MinIO 로컬 접속 정보도 동일.
> 환경변수 등록 상세는 팀 노션 "환경변수(시크릿) 정리" 표를 참고하세요.
> Windows에서 `setx`로 등록한 경우, 터미널·IDE를 **완전히 재시작**해야 반영됩니다.

### 3. 실행

```bash
./gradlew clean bootRun
```

> Flyway가 기동 시 `db/migration`(V1~)을 순차 적용하고, 로컬 전용 시드(`db/seed`, V100~)를 이어서 적용합니다. **시드는 fresh DB 전제** — 기존 로컬 DB가 있으면 드롭 후 재기동하세요.

### 4. API 문서 (Swagger)

기동 후 브라우저에서 접속:

```
http://localhost:8080/swagger-ui.html
```

- 우상단 **Authorize** 버튼에 로그인으로 발급받은 Access Token을 입력하면 보호 API를 테스트할 수 있습니다.
- ⚠️ 프론트(5173)가 아니라 **백엔드 포트(8080)로 직접** 접속합니다.

### 5. 회원가입 동작 확인

```
POST /api/auth/email/send    → 인증 코드 발송 (메일 수신 확인)
POST /api/auth/email/verify  → 인증 코드 검증
POST /api/auth/signup        → 회원가입 (201 Created)
```

---

## 📂 프로젝트 구조

도메인형 구조(계층형 아님). "어느 서비스에서 예외를 throw 하는가"가 소속 도메인 기준입니다.

```
com.paw.ddasoom
├── common          # 공통 모듈 (도메인에 속하지 않는 인프라)
│   ├── config      # RedisConfig, MinioConfig, SwaggerConfig, QueryDslConfig 등
│   ├── dto         # ApiResponse, PageResponse (공통 응답 규격)
│   ├── exception   # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── security    # SecurityConfig, SecurityConstants, AuthJwtTokenFilter, CustomUserDetails
│   └── util        # BaseTimeEntity, SecurityUtil
├── auth            # 인증 (회원가입, 로그인, 이메일 인증, 토큰 재발급, OAuth2, 비밀번호 재설정)
├── member          # 회원 (내 정보, 마이페이지, 관리자 회원 관리, 상태 제재)
├── dashboard       # 관리자 대시보드 (처리대기 지표, 현재 스냅샷)
├── statistics      # 관리자 통계 (기간 추세·분포 집계 — QueryDSL 단일 창구)
├── animal          # 유기동물 (공공 API 미러링, 목록/상세, 좋아요)
├── foster          # 임시보호 신청 (상태 전이, 심사)
├── board           # 커뮤니티 게시판 (강아지/고양이 정보, 입양 후기)
├── image           # 이미지 (MinIO 업로드, 공개/비공개 버킷, 하이브리드 연결)
├── qna             # 1:1 문의 (대화형 구조)
├── report          # 신고 (게시글/댓글/회원 대상, 승인 시 대상 숨김)
├── event           # 이벤트 (참여 집계)
└── support         # 공지사항 / FAQ
```

### 인증/인가 핵심 요약

- **AT**: 무저장(로그아웃 jti 블랙리스트만 예외), 클라이언트 전역 상태 변수, `Authorization: Bearer` 헤더
- **RT**: HttpOnly 쿠키 + Redis 이중 관리, 로테이션 + grace(30초)로 멀티탭 경합 흡수
- **탈퇴/강제탈퇴**: soft delete + `forceLogout` 마커로 기발급 AT까지 즉시 무효화
- **인가**: 기본 잠금(등록 안 된 API는 인증 필수) + `SecurityConstants` 등록제

---

## 🗄️ DB 마이그레이션 (Flyway)

- **운영 스키마**: `db/migration/V1~` — 한 번 머지된 마이그레이션은 수정하지 않고 새 버전으로 ALTER
- **로컬 시드**: `db/seed/V100~` — 개발/데모용 더미. ALTER 반영 시 자유롭게 재작성
- 시드는 상대 날짜(`CURDATE()` 등) 기반이라 언제 재생성해도 통계가 유효

---

## 📚 팀 문서

| 문서 | 내용 |
|------|------|
| [백엔드 코드 컨벤션](./docs/BACKEND_CODE_CONVENTIONS.md) | 패키지 구조, 네이밍, 예외 처리, API 공통 규격, 트랜잭션 규칙 |
| [DB 컨벤션](./docs/DB_CONVENTIONS.md) | 테이블/컬럼 명명, 시간 컬럼 정책, soft delete, 인덱스 규칙 |
| [Security Flow](./docs/SECURITY-FLOW_V3.md) | 인증/인가 흐름, 토큰 정책, 컨트롤러에서 로그인 회원 정보 꺼내는 법 |
| [Image Flow](./docs/IMAGE_FLOW.md) | 이미지 업로드 구조(하이브리드/버킷 분리), 구현 가이드 |

---

## 👥 팀

| 역할 | 담당 |
|------|-----|
| 유기동물 API 연동 / 조회, 배치 | 김종식 |
| 임시보호 신청 | 김경우 |
| 커뮤니티 / 입양 후기 (게시판) | 유창호 |
| **회원 / 인증 / 대시보드 / 통계 (Auth · Member · Security · Dashboard · Statistics)** | 구지훈 |
| 1:1 문의 / 신고 / 공지 / FAQ | 이서진 |
