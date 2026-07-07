# 🖼️ 따숨 Image Flow — 구현 참고 가이드

> 이미지 시스템을 **직접 구현하기 위한 참고 문서**입니다. 완성 코드가 아니라 각 클래스의 책임, 설계 포인트, 구현 순서, 검증 방법을 정리했습니다.
>
> 🔑 **확정 결정 (2026-07)**: ① 게시글 이미지 = 에디터 연동 하이브리드(본문 중간 삽입) ② 1:1 문의 = 첨부 형식 + 비공개 ③ 버킷 분리(`ddasoom-public` / `ddasoom-private`) ④ 파일 크기 10MB. (결정 배경은 부록 참고)
>
> 📐 준수 문서: `CONVENTIONS.md`(예외 3단계 패턴, DTO 규칙), `ddasoom_db_convention.md`(복수형 테이블, deleted_at 단일 soft delete)

---

## 1. 전체 그림

```
[게시글 — 하이브리드]
작성 중: 에디터 훅 → POST /api/images (file, ownerType=POST)
         → public 버킷 업로드 → owner_id NULL(임시)로 DB 저장 → 영구 URL + imageId 반환
         → 에디터가 URL을 <img>로 본문 삽입
저장 시: POST /api/posts (본문 + imageIds) → 게시글 INSERT → imageIds 확정 연결
수정 시: 최종 imageIds 전송 → diff (신규 연결 + 제외분 soft delete)

[1:1 문의 — 첨부]
업로드(ownerType=QNA) → private 버킷 → 문의 저장 시 확정 연결
조회 시 매번 Presigned URL(30분) 생성해 응답에 포함
```

핵심 규칙 두 가지:
- **URL 분기**: 공개(POST/NOTICE/ANIMAL) = 영구 정적 URL, 비공개(QNA) = Presigned 30분
- **임시 → 확정**: 업로드 시점엔 소유자가 없으므로 `owner_id NULL`로 저장, 소유자 저장 시점에 연결

---

## 2. 패키지 구조 (도메인형 구조 준수)

```
com.paw.ddasoom.image
├── controller/  ImageController        # POST /api/images 하나만
├── service/     ImageService           # 팀원용 공개 API — 도메인들은 이것만 호출
├── repository/  ImageRepository
├── domain/      Image, OwnerType
├── dto/response/ ImageResponse         # 요청 DTO 없음 (multipart 파라미터로 충분)
├── exception/   ImageException, ImageErrorCode
└── util/        MinioUtil              # 한 도메인만 사용 → image 하위 (미리 common 승격 금지)
```

---

## 3. 클래스별 설계 가이드

### 3-1. `OwnerType` (enum)

| 상수 | isPublic |
|------|----------|
| `POST`, `NOTICE`, `ANIMAL` | true |
| `QNA` | false |

- `boolean isPublic` 필드 하나를 갖는 enum. 버킷/URL 분기의 유일한 기준점.
- DB에는 `@Enumerated(EnumType.STRING)` — VARCHAR(20) (DB 컨벤션 6장: ENUM 타입 금지)

### 3-2. `Image` (엔티티)

| 컬럼 | 설계 |
|------|------|
| `image_id` | PK, BIGINT AUTO_INCREMENT |
| `owner_type` | VARCHAR(20), NOT NULL |
| `owner_id` | BIGINT, **NULL 허용** — NULL = 임시(미확정) 상태 |
| `object_key` | MinIO 객체 키, NOT NULL |
| `original_name` | 원본 파일명, NOT NULL |
| `deleted_at` | soft delete (NULL = 활성) |

설계 포인트:
- 테이블명 `images` **복수형** (DB 컨벤션 준수 — 기존 `member` 테이블이 단수인 건 알려진 불일치이므로 따라가지 않음)
- soft delete는 `deleted_at` **단일 방식** (`Member`의 isDeleted 이중 패턴 미적용 — 팀 결정)
- 폴리모픽 논리 참조라 FK 자동 인덱스가 없음 → `@Table(indexes = ...)`로 `idx_images_owner (owner_type, owner_id)` **수동 지정 필수** (DB 컨벤션 8장)
- `BaseTimeEntity` 상속, `@NoArgsConstructor(PROTECTED)` + `@Builder`, `@Setter` 금지
- 리치 도메인 메서드 2개:
  - `attachTo(OwnerType, Long ownerId)` — 확정 연결. **내부에서 검증**: 삭제된 이미지면 `IMAGE_NOT_FOUND`, ownerType 불일치·이미 다른 소유자에 연결이면 `IMAGE_OWNER_MISMATCH`. 같은 소유자에 재연결은 no-op (수정 diff에서 재사용됨)
  - `softDelete()` — `deletedAt = now()`

### 3-3. `ImageErrorCode` / `ImageException`

CONVENTIONS.md 5장의 3단계 패턴 그대로 (`FosterErrorCode` 예시 참조).

| enum 상수 | code | HTTP | 상황 |
|-----------|------|------|------|
| `INVALID_IMAGE_TYPE` | IMAGE_001 | 400 | jpeg/png/gif/webp 외 형식 |
| `IMAGE_SIZE_EXCEEDED` | IMAGE_002 | 400 | 10MB 초과 |
| `IMAGE_COUNT_EXCEEDED` | IMAGE_003 | 400 | 소유자당 10장 초과 |
| `IMAGE_NOT_FOUND` | IMAGE_004 | 404 | 없거나 삭제된 imageId |
| `IMAGE_UPLOAD_FAILED` | IMAGE_005 | 500 | MinIO 업로드/URL 발급 실패 |
| `IMAGE_OWNER_MISMATCH` | IMAGE_006 | 400 | 다른 소유자의 imageId 전달 |

### 3-4. `MinioUtil`

**책임: 버킷 선택, 객체 키 생성, URL 방식 분기를 전부 캡슐화.** 도메인 코드가 버킷/키를 알 필요 없게 만드는 것이 목표.

| 메서드 | 시그니처(권장) | 동작 |
|--------|---------------|------|
| 업로드 | `String upload(MultipartFile, OwnerType)` | ownerType으로 버킷 선택 → 키 생성 → putObject → 객체 키 반환. 실패 시 `IMAGE_005` |
| URL 발급 | `String getUrl(OwnerType, String objectKey)` | 공개면 `{endpoint}/{bucket}/{key}` 정적 URL, 비공개면 Presigned 30분 |
| 초기화 | `@PostConstruct void initBuckets()` | 버킷 2개 자동 생성 + public 버킷 익명 읽기 정책 |

구현 포인트:
- 객체 키: `{yyyy}/{MM}/{용도}/{uuid}.{확장자}` — 용도는 `ownerType.name().toLowerCase()`. 키 생성은 private 메서드로 분리 (Redis 키 규칙과 동일한 원칙)
- Presigned 만료 30분은 `private static final` 상수 + 이유 주석 (매직 넘버 금지)
- 공개 정책 JSON은 **텍스트 블록(`"""`) + `formatted()`** (CONVENTIONS 3장). `Action`은 `s3:GetObject`만 — 익명 쓰기/삭제 차단
- ⚠️ **initBuckets는 try-catch로 감싸 실패 시 log.warn만** — MinIO 미기동 팀원의 서버가 함께 죽지 않게. (yml의 Redis autoconfigure 제외와 같은 방어 철학)
- MinIO SDK 예외는 checked가 많아 `catch (Exception e)` 후 `IMAGE_005` 변환 + `log.error` (스택트레이스 포함)

### 3-5. `ImageRepository`

Spring Data JPA 쿼리 메서드 2개면 충분:

```
findAllByOwnerTypeAndOwnerIdAndDeletedAtIsNull(OwnerType, Long)  → List<Image>
countByOwnerTypeAndOwnerIdAndDeletedAtIsNull(OwnerType, Long)    → long
```

### 3-6. `ImageService` — 팀원용 공개 API 4개

| 메서드 | 트랜잭션 | 책임 |
|--------|----------|------|
| `upload(MultipartFile, OwnerType)` → `ImageResponse` | `@Transactional` | 검증 → **MinIO 업로드 → DB INSERT 순서 고정** → URL 포함 응답 |
| `attach(List<Long> imageIds, OwnerType, Long ownerId)` | `@Transactional` | 생성 시 확정 연결. null/빈 리스트는 조기 return |
| `syncImages(List<Long> imageIds, OwnerType, Long ownerId)` | `@Transactional` | 수정 시 diff: 기존 활성 중 요청에 없는 것 softDelete → `attach()` 재사용 |
| `getImages(OwnerType, Long ownerId)` → `List<ImageResponse>` | `readOnly = true` | 활성 이미지 + URL 목록 |

구현 포인트:
- **업로드 순서가 MinIO → DB인 이유**: MinIO 실패 시 DB에 흔적이 남지 않음. 반대로 DB 실패 시 MinIO에 고아 객체가 남지만 DB 기준 정합성은 유지 (수용하는 트레이드오프 — 부록 참고)
- `attach()` 검증 3종: ① `findAllById` 결과 수 ≠ 요청 수 → `IMAGE_004` ② 각 이미지의 `attachTo()` 내부 검증(3-2 참고) ③ 연결 후 활성 개수 > 10 → `IMAGE_003` (초과 시 예외 → 트랜잭션 롤백으로 전체 취소)
- 장수 제한을 upload가 아닌 attach에서 검증하는 이유: 업로드 시점엔 소유자가 없어 "소유자당 10장"을 셀 수 없음
- `syncImages`는 삭제 처리 후 `attach()`를 그대로 호출 — 이미 연결된 이미지는 attachTo가 no-op이라 안전. 소유자 삭제 시 이미지 정리는 `syncImages(빈 리스트, ...)` 호출로 해결
- 파일 검증(private 메서드): 빈 파일 / 크기 10MB / 확장자 + Content-Type **이중 검증** (`Set.of(...)` 상수). 상수는 `MAX_FILE_SIZE`, `MAX_IMAGE_COUNT` 등 이유 주석과 함께

### 3-7. `ImageResponse` / `ImageController`

- `ImageResponse`: `imageId` + `url` 두 필드. `@Getter` + `@Builder` + 정적 팩토리 — 단, URL은 엔티티 밖에서 계산되므로 `from(Image, String url)` 형태 허용
- `ImageController`: `POST /api/images` 하나. `@RequestParam MultipartFile file` + `@RequestParam OwnerType ownerType` → 201 Created + `ApiResponse.success(...)`. try-catch 금지 (핸들러가 처리)

---

## 4. 설정 변경 (`application.yml` + `application.yml-example` 동시 갱신)

```yaml
spring:
  servlet:
    multipart:
      max-file-size: 11MB      # ⚠️ 10MB가 아님 — 아래 이유 참고
      max-request-size: 11MB

minio:
  endpoint: http://localhost:9000
  access-key: ...
  secret-key: ...
  bucket:
    public-name: ddasoom-public
    private-name: ddasoom-private
```

> ⚠️ **Spring 제한을 11MB로 두는 이유**: 10MB로 잡으면 초과 파일이 컨트롤러 도달 전에 Spring 예외로 터져 `GLOBAL_ERROR`(500)로 응답됩니다. 한 단계 크게 잡으면 `ImageService`의 비즈니스 검증이 먼저 걸려 `IMAGE_002`(400) 규격으로 응답할 수 있습니다.
> (대안이었던 GlobalExceptionHandler에 multipart 예외 핸들러 추가는 common → image 도메인 의존이 생겨 배제)

---

## 5. 구현 순서 체크리스트 (권장)

```
1. OwnerType + Image + ImageErrorCode/Exception
   → 검증: 서버 기동 시 images 테이블 생성, idx_images_owner 인덱스 확인 (SHOW INDEX FROM images)
2. yml 설정 + MinioUtil
   → 검증: 서버 기동 로그에 버킷 생성 확인, MinIO 콘솔에서 public 버킷 정책 확인
   → 검증: MinIO 끄고 기동 → 서버는 뜨고 warn 로그만 남는지
3. ImageRepository + ImageService.upload + ImageController
   → 검증: curl 업로드 → 201 + URL 응답, 브라우저에서 URL 접근 (POST=열림 / QNA는 presigned만 열림)
4. attach / syncImages / getImages
   → 검증: 아래 6장 시나리오
5. docs 문서/PR — 프론트 영향(⚠️ 응답 포맷, ownerType 파라미터) 표시
```

## 6. 수동 검증 시나리오

| # | 시나리오 | 기대 결과 |
|---|----------|-----------|
| 1 | POST ownerType=POST 업로드 후 URL 브라우저 접근 | 이미지 표시 (영구 URL) |
| 2 | ownerType=QNA 업로드 후 30분 뒤 URL 접근 | 403/만료 (Presigned) |
| 3 | pdf 업로드 / 11MB 초과 업로드 | IMAGE_001 / IMAGE_002 (400) |
| 4 | 존재하지 않는 imageId로 attach | IMAGE_004 |
| 5 | 게시글 A의 imageId를 게시글 B에 attach | IMAGE_006 |
| 6 | 11장 attach | IMAGE_003 + 전체 롤백 확인 |
| 7 | syncImages로 1장 제외 | 제외분 deleted_at 세팅, MinIO 객체는 잔존 |

## 7. 알려진 한계 (구현하지 않기로 한 것 — 추가 구현 금지)

| 한계 | 대응 |
|------|------|
| 고아 이미지 (업로드 후 미저장 이탈) | 정리 배치 **미구현** (데모 규모 판단). 필요 시 "owner_id NULL + 24h 경과" 스케줄러 |
| 업로더 검증 (누가 올렸는지) | JWT 구현 후 member_id 컬럼 + 검증 추가 [예정] |
| ownerType 오타 시 500 응답 | enum 변환 실패는 GLOBAL_ERROR로 떨어짐. 400 규격화는 공통 핸들러 작업이라 별도 건 |
| soft delete 후 public 객체 접근 가능 | 공개 콘텐츠 한정이라 수용. QNA는 private + Presigned라 해당 없음 |

---

## 부록. 결정 배경 요약

- **하이브리드 채택**: base64 직삽입은 DB 비대화(용량 33%↑, 캐싱 불가)로 배제. 첨부 형식은 본문 삽입 UX 불가. 하이브리드가 저장 구조 + UX 모두 확보
- **버킷 분리**: 1:1 문의 이미지의 개인정보 가능성 → 노출 방어선을 정책 설정이 아닌 인프라 구조에 둠. 부수 효과로 날짜 시작 키 형식 유지 가능 (prefix 정책 불필요해짐)
- **공개 버킷 정적 URL**: 본문에 박히는 URL은 영구여야 함. URL 치환·서버 프록시 대비 조회 비용 0 → 기존 "MinIO 조회 방식" 팀 안건 종결
- **10MB**: 모바일 원본 사진(3~8MB) 대응 + 유기동물 사진이 핵심 콘텐츠. 성능 이슈 시 프론트 리사이징 별도 협의
- **날짜 키 `{yyyy}/{MM}/{용도}/{uuid}`**: 기간별 백업·운영 관리 용이. 일(dd)은 탐색 깊이만 늘려 제외, 볼륨 증가 시 재검토
- **MinIO → DB 순서**: 실패 시 "DB에 있는데 파일이 없는" 상태(조회 깨짐)보다 "파일만 있는" 상태(고아 객체)가 안전
