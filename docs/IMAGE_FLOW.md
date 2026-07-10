# 🖼️ 따숨 Image Flow — 구현 참고 가이드

> 이미지 시스템을 **직접 구현하기 위한 참고 문서**입니다. 완성 코드가 아니라 각 클래스의 책임, 설계 포인트, 구현 순서, 검증 방법을 정리했습니다.
>
> 🔑 **확정 결정 (2026-07)**: ① 게시글 이미지 = 에디터 연동 하이브리드(본문 중간 삽입) ② 1:1 문의 = 첨부 형식 + 비공개 ③ 버킷 분리(`ddasoom-public` / `ddasoom-private`) ④ 파일 크기 10MB ⑤ 대표 이미지(`is_thumbnail`) = 사용자 명시적 지정. (①~④ 근거는 `이미지 시스템 주요 결정 사항 및 근거` 문서, ⑤ 및 필드 정정 근거는 하단 부록 2 참고)
>
> 🔄 **개정 (2026-07-10)**: 실제 DB 설계(`Ddasoom.sql`) 반영 — 컬럼명 정정(`image_key`, `original_file_name`), `file_size`/`mime_type`/`image_order`/`is_thumbnail` 컬럼 추가. `owner_id`는 SQL상 NOT NULL로 표기되어 있으나, 하이브리드 업로드 플로우(결정 2 — "owner_id 없이 임시 저장")와 정면 충돌하여 **NULL 허용을 유지**하기로 확정 (SQL 표기는 초안 단계 오기로 판단).
>
> 📐 준수 문서: `CONVENTIONS.md`(예외 3단계 패턴, DTO 규칙), `ddasoom_db_convention.md`(단수형 테이블, deleted_at 단일 soft delete), `이미지 시스템 주요 결정 사항 및 근거`(하이브리드/버킷분리/파일크기/키형식 결정 배경)

---

## 1. 전체 그림

```
[게시글 — 하이브리드]
작성 중: 에디터 훅 → POST /api/images (file, ownerType=POST)
         → public 버킷 업로드 → owner_id NULL(임시)로 DB 저장 → 영구 URL + imageId 반환
         → 에디터가 URL을 <img>로 본문 삽입
저장 시: POST /api/posts (본문 + imageIds + thumbnailImageId) → 게시글 INSERT → imageIds 확정 연결 → 대표 이미지 지정
수정 시: 최종 imageIds + thumbnailImageId 전송 → diff (신규 연결 + 제외분 soft delete) → 대표 이미지 재지정

[1:1 문의 — 첨부]
업로드(ownerType=QNA) → private 버킷 → 문의 저장 시 확정 연결
조회 시 매번 Presigned URL(30분) 생성해 응답에 포함
```

핵심 규칙 세 가지:
- **URL 분기**: 공개(POST/NOTICE/ANIMAL) = 영구 정적 URL, 비공개(QNA) = Presigned 30분
- **임시 → 확정**: 업로드 시점엔 소유자가 없으므로 `owner_id NULL`로 저장, 소유자 저장 시점에 연결
- **대표 이미지는 사용자가 지정**: 업로드 시점에 자동 결정하지 않음. 소유자 확정과 동시에(또는 이후 수정 시) 명시적으로 지정

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
| `owner_type` | VARCHAR(20~30), NOT NULL |
| `owner_id` | BIGINT, **NULL 허용** — NULL = 임시(미확정) 상태 |
| `image_key` | MinIO 객체 키, NOT NULL (기존 `object_key`에서 명칭 통일) |
| `original_file_name` | 원본 파일명, NOT NULL (기존 `original_name`에서 명칭 통일) |
| `file_size` | BIGINT UNSIGNED, NOT NULL — 업로드 검증(10MB) 시 계산한 값을 그대로 저장 |
| `mime_type` | VARCHAR(100), NOT NULL — 업로드 검증(확장자+Content-Type) 시 확인한 값을 그대로 저장 |
| `image_order` | INT UNSIGNED, NOT NULL DEFAULT 0 — 노출 순서. **`attach()` 호출 시 요청된 imageIds 리스트의 인덱스로 자동 설정** (2026-07-10 결정) |
| `is_thumbnail` | BOOLEAN, NOT NULL DEFAULT FALSE — 대표 이미지 여부. 소유자당 최대 1개만 true |
| `deleted_at` | soft delete (NULL = 활성) |

설계 포인트:
- 테이블명 `image` **단수형** (`ddasoom_db_convention.md` 2026-07-07 개정 — 복수형→단수형 변경 — 준수. 이 문서의 기존 "복수형 준수" 서술은 개정 전 기준이었으므로 정정)
- soft delete는 `deleted_at` **단일 방식** (`Member`의 isDeleted 이중 패턴 미적용 — 팀 결정)
- 폴리모픽 논리 참조라 FK 자동 인덱스가 없음 → `@Table(indexes = ...)`로 `idx_image_owner (owner_type, owner_id)` **수동 지정 필수** (DB 컨벤션 8장, 인덱스명도 테이블명과 동일하게 단수형 통일)
- `BaseTimeEntity` 상속, `@NoArgsConstructor(PROTECTED)` + `@Builder`, `@Setter` 금지
- ⚠️ **`owner_id`는 NOT NULL이 아니라 NULL 허용을 유지합니다.** `Ddasoom (2).sql` 초안에는 NOT NULL로 표기돼 있으나, 이는 하이브리드 업로드 플로우(결정 2 — "owner_id 없이 임시 저장")와 정면으로 충돌합니다. 결정 문서가 날짜 있는 팀 합의 기록이고 SQL 초안 쪽에 오타(`AUTO_INCTREMENT`, `VACHAR` 등)가 다수 있는 점을 근거로, SQL의 NOT NULL 표기를 오기로 판단해 기존 설계를 유지합니다. **실제 DDL 작성 전 팀 확인 한 번 더 권장.**
- 리치 도메인 메서드:
  - `attachTo(OwnerType, Long ownerId)` — 확정 연결. **내부에서 검증**: 삭제된 이미지면 `IMAGE_NOT_FOUND`, ownerType 불일치·이미 다른 소유자에 연결이면 `IMAGE_OWNER_MISMATCH`. 같은 소유자에 재연결은 no-op (수정 diff에서 재사용됨)
  - `softDelete()` — `deletedAt = now()`
  - `markAsThumbnail()` — `isThumbnail = true`. 삭제된 이미지면 `IMAGE_NOT_FOUND`
  - `unmarkAsThumbnail()` — `isThumbnail = false`
  - `updateOrder(int order)` — `imageOrder = order`. `attach()` 호출마다 매번 갱신 (재연결이어도 순서는 갱신)

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

대표 이미지 지정(`setThumbnail`)도 이 코드를 재사용합니다 — 신규 코드 불필요 (미존재 imageId → `IMAGE_004`, 소유자 불일치 → `IMAGE_006`).

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

Spring Data JPA 쿼리 메서드 4개면 충분:

```
findAllByOwnerTypeAndOwnerIdAndDeletedAtIsNullOrderByImageOrderAsc(OwnerType, Long)       → List<Image>   # image_order 오름차순 정렬
countByOwnerTypeAndOwnerIdAndDeletedAtIsNull(OwnerType, Long)                             → long
findByOwnerTypeAndOwnerIdAndIsThumbnailTrueAndDeletedAtIsNull(OwnerType, Long)             → Optional<Image>   # 기존 대표 이미지 조회(해제용)
findAllByOwnerTypeAndOwnerIdInAndIsThumbnailTrueAndDeletedAtIsNull(OwnerType, List<Long>)  → List<Image>       # 목록 조회 배치용(N+1 방지)
```

### 3-6. `ImageService` — 팀원용 공개 API 5개

| 메서드 | 트랜잭션 | 책임 |
|--------|----------|------|
| `upload(MultipartFile, OwnerType)` → `ImageResponse` | `@Transactional` | 검증 → **MinIO 업로드 → DB INSERT 순서 고정** → `file_size`/`mime_type`은 MultipartFile에서 그대로 추출해 저장 → URL 포함 응답 |
| `attach(List<Long> imageIds, OwnerType, Long ownerId)` | `@Transactional` | 생성 시 확정 연결. **리스트 순서(index)를 `image_order`로 저장** (재연결 시에도 순서 갱신). null/빈 리스트는 조기 return |
| `syncImages(List<Long> imageIds, OwnerType, Long ownerId)` | `@Transactional` | 수정 시 diff: 기존 활성 중 요청에 없는 것 softDelete → `attach()` 재사용 |
| `setThumbnail(Long imageId, OwnerType, Long ownerId)` | `@Transactional` | 기존 대표 이미지 조회 후 `unmarkAsThumbnail()` → 대상 이미지 소유자 검증(`IMAGE_OWNER_MISMATCH`) → `markAsThumbnail()`. 소유자당 1개만 true인 상태를 애플리케이션 레벨에서 보장 (MySQL은 partial unique index 미지원) |
| `getImages(OwnerType, Long ownerId)` → `List<ImageResponse>` | `readOnly = true` | 활성 이미지 + URL + `isThumbnail` 목록 |

구현 포인트:
- **업로드 순서가 MinIO → DB인 이유**: MinIO 실패 시 DB에 흔적이 남지 않음. 반대로 DB 실패 시 MinIO에 고아 객체가 남지만 DB 기준 정합성은 유지 (수용하는 트레이드오프 — 부록 참고)
- `attach()` 검증 3종: ① `findAllById` 결과 수 ≠ 요청 수 → `IMAGE_004` ② 각 이미지의 `attachTo()` 내부 검증(3-2 참고) ③ 연결 후 활성 개수 > 10 → `IMAGE_003` (초과 시 예외 → 트랜잭션 롤백으로 전체 취소)
- 장수 제한을 upload가 아닌 attach에서 검증하는 이유: 업로드 시점엔 소유자가 없어 "소유자당 10장"을 셀 수 없음
- `syncImages`는 삭제 처리 후 `attach()`를 그대로 호출 — 이미 연결된 이미지는 attachTo가 no-op이라 안전. 소유자 삭제 시 이미지 정리는 `syncImages(빈 리스트, ...)` 호출로 해결
- **`image_order`는 우선 업로드(=본문 삽입) 순서를 그대로 사용** — `attach()`가 매 호출마다 요청 리스트의 인덱스로 순서를 갱신하므로, 추후 드래그 재정렬이 필요해지면 프론트가 재정렬된 imageIds를 `syncImages()`에 다시 보내는 것만으로 동작. **별도 재정렬 API 불필요** (2026-07-10 결정). `findAllById` 결과는 순서를 보장하지 않으므로, `Map<Long, Image>`로 변환 후 imageIds 순서대로 순회하며 `updateOrder(i)` 호출
- **`setThumbnail`은 `attach`/`syncImages`와 별도 메서드로 분리** — 썸네일은 POST 도메인에만 의미 있는 개념이라, QNA 등 다른 ownerType 호출부까지 파라미터를 오염시키지 않기 위함. PostService에서 같은 트랜잭션 안에서 순차 호출 (Spring 기본 전파 REQUIRED로 자연히 합류)
- 수정 시 썸네일로 지정된 이미지가 새 imageIds에서 빠져 `syncImages`로 soft delete되면, 대표 이미지가 사라짐 — 프론트가 재지정하도록 유도 (백엔드가 자동으로 다른 이미지를 대표로 승격하지 않음, 명시적 지정 원칙 유지)
- 파일 검증(private 메서드): 빈 파일 / 크기 10MB / 확장자 + Content-Type **이중 검증** (`Set.of(...)` 상수). 상수는 `MAX_FILE_SIZE`, `MAX_IMAGE_COUNT` 등 이유 주석과 함께

### 3-7. `ImageResponse` / `ImageController`

- `ImageResponse`: `imageId` + `url` + `isThumbnail` 세 필드. `@Getter` + `@Builder` + 정적 팩토리 — 단, URL은 엔티티 밖에서 계산되므로 `from(Image, String url)` 형태 허용
- `ImageController`: `POST /api/images` 하나. `@RequestParam MultipartFile file` + `@RequestParam OwnerType ownerType` → 201 Created + `ApiResponse.success(...)`. try-catch 금지 (핸들러가 처리)
- 대표 이미지 지정을 위한 별도 엔드포인트는 두지 않음 — `POST /api/posts`, `PATCH /api/posts/{id}` 요청 바디의 `thumbnailImageId` 필드로 처리 (board 도메인 컨트롤러에서 `ImageService.setThumbnail()` 호출)

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
   → 검증: 서버 기동 시 image 테이블 생성, idx_image_owner 인덱스 확인 (SHOW INDEX FROM image)
2. yml 설정 + MinioUtil
   → 검증: 서버 기동 로그에 버킷 생성 확인, MinIO 콘솔에서 public 버킷 정책 확인
   → 검증: MinIO 끄고 기동 → 서버는 뜨고 warn 로그만 남는지
3. ImageRepository + ImageService.upload + ImageController
   → 검증: curl 업로드 → 201 + URL 응답, 브라우저에서 URL 접근 (POST=열림 / QNA는 presigned만 열림)
4. attach / syncImages / getImages / setThumbnail
   → 검증: 아래 6장 시나리오
5. docs 문서/PR — 프론트 영향(⚠️ 응답 포맷, ownerType 파라미터, thumbnailImageId 파라미터) 표시
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
| 8 | imageId A를 썸네일로 지정 후 imageId B를 썸네일로 재지정 | A는 is_thumbnail=false로 자동 해제, B만 true |

## 7. 알려진 한계 (구현하지 않기로 한 것 — 추가 구현 금지)

| 한계 | 대응 |
|------|------|
| 고아 이미지 (업로드 후 미저장 이탈) | 정리 배치 **미구현** (데모 규모 판단). 필요 시 "owner_id NULL + 24h 경과" 스케줄러 |
| 업로더 검증 (누가 올렸는지) | JWT 구현 후 member_id 컬럼 + 검증 추가 [예정] |
| ownerType 오타 시 500 응답 | enum 변환 실패는 GLOBAL_ERROR로 떨어짐. 400 규격화는 공통 핸들러 작업이라 별도 건 |
| soft delete 후 public 객체 접근 가능 | 공개 콘텐츠 한정이라 수용. QNA는 private + Presigned라 해당 없음 |
| 드래그 재정렬 UI/API | 백엔드는 `syncImages()` 재사용으로 대비돼 있으나, 프론트 드래그 정렬 UI 자체는 **미구현** (필요 시점에 별도 작업) |

---

## 부록 1. 결정 배경 요약 (①~④)

- **하이브리드 채택**: base64 직삽입은 DB 비대화(용량 33%↑, 캐싱 불가)로 배제. 첨부 형식은 본문 삽입 UX 불가. 하이브리드가 저장 구조 + UX 모두 확보
- **버킷 분리**: 1:1 문의 이미지의 개인정보 가능성 → 노출 방어선을 정책 설정이 아닌 인프라 구조에 둠. 부수 효과로 날짜 시작 키 형식 유지 가능 (prefix 정책 불필요해짐)
- **공개 버킷 정적 URL**: 본문에 박히는 URL은 영구여야 함. URL 치환·서버 프록시 대비 조회 비용 0 → 기존 "MinIO 조회 방식" 팀 안건 종결
- **10MB**: 모바일 원본 사진(3~8MB) 대응 + 유기동물 사진이 핵심 콘텐츠. 성능 이슈 시 프론트 리사이징 별도 협의
- **날짜 키 `{yyyy}/{MM}/{용도}/{uuid}`**: 기간별 백업·운영 관리 용이. 일(dd)은 탐색 깊이만 늘려 제외, 볼륨 증가 시 재검토
- **MinIO → DB 순서**: 실패 시 "DB에 있는데 파일이 없는" 상태(조회 깨짐)보다 "파일만 있는" 상태(고아 객체)가 안전

## 부록 2. 이번 개정(2026-07-10)에서 반영/보류한 사항

| 항목 | 처리 | 근거 |
|------|------|------|
| `owner_id` NULL 허용 | **유지 확정** | SQL 초안의 NOT NULL 표기는 결정 2("owner_id 없이 임시 저장")와 충돌 → 결정 문서 우선 |
| 테이블/인덱스명 단수형 (`image`, `idx_image_owner`) | **정정 반영** | `ddasoom_db_convention.md` 2026-07-07 개정(복수형→단수형)과 실제 SQL에 맞춤 |
| `is_thumbnail` 명시적 지정 | **설계 반영** | 사용자가 직접 선택하는 방식으로 확정 — 자동 지정(첫 업로드 이미지) 대비 UX상 정확도 우선 |
| `image_order` 사용 방식 | **결정 (2026-07-10 추가 확정)** | 우선 업로드 순서(=attach 시 리스트 인덱스) 사용, 필요 시 프론트가 재정렬된 imageIds를 syncImages로 재전송 — 드래그 재정렬 확장 시 백엔드 변경 불필요 |
| `file_size` / `mime_type` 저장 | **반영** | 기존 업로드 검증 로직에서 이미 계산하는 값이라 추가 비용 없이 영속화 |