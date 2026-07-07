# 🗄️ ddasoom DB 컨벤션

> 팀원 모두가 테이블/컬럼 설계 및 리뷰 시 따라야 하는 규칙입니다.
> 마지막 수정: 2026-07-07 (테이블명 복수형 → 단수형 변경)

---

## 1️⃣ 명명 규칙 (Naming Convention)

- **`스네이크 케이스(snake_case)`** 사용
  - 예: `user_profile`, `order_history`
- 파스칼/카멜 케이스 금지 (`Field`, `userName` ❌)
- MySQL 예약어 사용 금지 (`order`, `group`, `key` 등)
- `data`, `info`, `field` 같은 무의미한 이름 금지

---

## 2️⃣ 테이블(Table) 명명 규칙

- **`단수형`** 사용 (엔티티 클래스명과 1:1 대응, PK/FK 컬럼명 규칙과 일관)
  - 예: `member`, `post`, `animal`, `foster`
- 연결(매핑) 테이블도 단수형: `member_social`, `animal_like`
- 모든 테이블에 `COMMENT` 필수

---

## 3️⃣ 컬럼(Column) 명명 규칙

### 기본키 (Primary Key)

- **`{테이블명}_id`** 형태로 통일 (조인 시 혼동 방지, 테이블명이 단수이므로 그대로 사용)
  - 예: `member_id`, `post_id`, `animal_id`

### 외래키 (Foreign Key)

- **`{참조테이블명}_id`** 형태
  - 예: `post` 테이블에서 `member` 참조 ➔ `member_id`
- 같은 테이블을 두 번 참조할 때는 **역할 기반 이름** 사용
  - 예: `reviewer_id`, `answered_id` (둘 다 `member` 참조)

### 날짜/시간 컬럼

- 과거형 동사/이벤트명 뒤에 **`_at`** 접미사
  - 예: `created_at`, `updated_at`, `deleted_at`, `answered_at`, `foster_start_at`
- 시간이 필요 없는 날짜는 `_date` 접미사 + DATE 타입 허용

### 여부(Boolean) 컬럼

- **`is_`, `has_`, `use_`** 접두사 사용
  - 예: `is_visible`, `is_thumbnail`, `is_extended`

### 카운트 컬럼

- **`_count`** 접미사 + `INT UNSIGNED DEFAULT 0`
  - 예: `view_count`, `comment_count`

---

## 4️⃣ 제약조건(Constraint) 명명 규칙

| 제약 조건 종류 | 약어 | 형식 | 예시 |
| --- | --- | --- | --- |
| Primary Key | `pk` | `pk_{테이블}` | `pk_member` |
| Foreign Key | `fk` | `fk_{테이블}_{참조대상/역할}` | `fk_post_member`, `fk_foster_reviewer` |
| Unique | `uk` | `uk_{테이블}_{컬럼}` | `uk_member_email` |
| Index | `idx` | `idx_{테이블}_{컬럼들}` | `idx_image_owner` |

---

## 5️⃣ 시간(날짜/시간) 컬럼 규칙

### 📌 기본 원칙

> **시간의 주인은 애플리케이션(JPA Auditing), DB DEFAULT는 안전망으로만 사용한다.**

### 타입 규칙

- 모든 시간 컬럼은 **`DATETIME(6)`** 으로 통일 (마이크로초 정밀도)
  - Java `LocalDateTime`과 정밀도를 맞춰 저장↔조회 값 불일치 방지
- `TIMESTAMP` 타입 사용 금지 (2038년 문제, 암묵적 timezone 변환)
- DEFAULT 표기는 **`CURRENT_TIMESTAMP(6)`** 만 사용
  - `NOW()` 등 다른 표기 금지, 컬럼 정밀도와 DEFAULT 정밀도 일치 필수

### 감사(Audit) 컬럼 — 모든 테이블 공통

| 컬럼 | 타입 | NULL | DEFAULT | 관리 주체 |
| --- | --- | --- | --- | --- |
| `created_at` | DATETIME(6) | NOT NULL | CURRENT_TIMESTAMP(6) | JPA `@CreatedDate` (DEFAULT는 안전망) |
| `updated_at` | DATETIME(6) | NOT NULL | 없음 | JPA `@LastModifiedDate` |
| `deleted_at` | DATETIME(6) | NULL | 없음 | 서비스 로직 (soft delete) |

- `updated_at`은 생성 시점에 `created_at`과 동일한 값으로 세팅 (JPA Auditing 기본 동작)
- `deleted_at`은 **NULL = 활성 상태**
- **`ON UPDATE CURRENT_TIMESTAMP` 사용 금지** — updated_at 갱신 여부는 애플리케이션이 결정
- 감사 컬럼 3종은 `BaseEntity`(@MappedSuperclass) 상속으로 통일 관리
- 컬럼 배치 순서: `created_at → updated_at → deleted_at` 고정

```java
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, columnDefinition = "DATETIME(6)")
    private LocalDateTime updatedAt;
}
```

### 도메인 시간 컬럼

- 비즈니스 이벤트 시각: `answered_at`, `foster_start_at`, `foster_end_at` 등
- NULL 허용 — **이벤트 발생 전 = NULL**
- DEFAULT 부여 금지, **서비스 로직에서 명시적으로 세팅** (자동화 금지)

### Soft Delete 규칙

- 삭제 = `deleted_at`에 현재 시각 세팅 (물리 DELETE 금지)
- 복구 = `deleted_at = NULL`
- soft delete도 '수정'으로 간주 → updated_at 함께 갱신 허용
- 활성 데이터 조회 조건: `WHERE deleted_at IS NULL`

### Timezone 규칙

> **DB · JDBC · JVM 세 곳의 timezone은 반드시 일치시킨다. (Asia/Seoul 고정)**

| 위치 | 설정 |
| --- | --- |
| MySQL | `default_time_zone = '+09:00'` |
| JDBC URL | `serverTimezone=Asia/Seoul` |
| JVM / Spring | `-Duser.timezone=Asia/Seoul` |
| Java 타입 | `LocalDateTime` |

---

## 6️⃣ 데이터 타입 규칙

### 문자열

- ID/코드성 값: `VARCHAR` + 적정 길이 (예: provider `VARCHAR(20)`)
- 제목류: `VARCHAR(255)` / 본문류: `TEXT`
- 고정 길이 코드(성별 M/F/Q 등): `CHAR(1)`
- **외부 API의 숫자형 문자열 ID는 VARCHAR로 저장** (예: 유기번호 15자리 → INT 초과)

### 숫자

- PK/FK: `BIGINT`
- 음수 불가 값(카운트, 파일 크기): **`UNSIGNED`** 부여
- 소수점 가능성 있는 값(몸무게 등): `DECIMAL(p,s)` (FLOAT 금지)
- 연도: `SMALLINT` (TINYINT는 255까지라 불가)

### ENUM 대신 VARCHAR

> **DB는 `VARCHAR(20~30)`, 값 검증은 Java Enum(`@Enumerated(EnumType.STRING)`)으로 한다.**

- 이유: ENUM은 값 추가 시 `ALTER TABLE` 필요 → 운영 부담
- 값은 **대문자 스네이크 케이스**로 통일: `PENDING`, `ADOPTION_REVIEW`
- 허용 값 목록은 반드시 컬럼 COMMENT에 명시

```sql
`status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED'
```

### Boolean

- `BOOLEAN` (TINYINT(1)) + `NOT NULL DEFAULT FALSE(또는 TRUE)`

---

## 7️⃣ 제약조건 & 무결성 규칙

### FK 규칙

- 참조 관계가 있는 컬럼은 **FK 제약조건 필수** (논리 FK만 두는 것 금지)
  - 예외: 폴리모픽 테이블(`image`의 `owner_id`)만 논리 참조 허용
- 삭제 정책 기본값: **`ON DELETE RESTRICT`** (soft delete 중심이므로 물리 삭제 방지 안전망)

### UNIQUE 규칙

- 비즈니스적으로 유일해야 하는 값은 **DB UNIQUE 제약으로 강제** (애플리케이션 검증만으로 불충분)
  - 예: `email`, `nickname`, `desertion_no`, `(provider, provider_id)`, `(animal_id, member_id)` 좋아요
- **복합 PK로 중복 방지 금지** — PK는 단일 AUTO_INCREMENT, 중복 방지는 UNIQUE 제약으로 분리

```sql
-- ❌ PRIMARY KEY (animal_like_id, animal_id, member_id)
-- ✅ PRIMARY KEY (animal_like_id) + UNIQUE (animal_id, member_id)
```

---

## 8️⃣ 인덱스 규칙

- FK 컬럼: InnoDB가 자동 생성하므로 별도 불필요
- **폴리모픽 테이블은 수동 인덱스 필수** (FK가 없어 자동 생성 안 됨)

```sql
CREATE INDEX `idx_image_owner` ON `image` (`owner_type`, `owner_id`);
```

- 목록 조회 패턴에는 복합 인덱스 부여 (필터 컬럼 → 정렬 컬럼 순)

```sql
CREATE INDEX `idx_post_board_created` ON `post` (`board_type`, `created_at`);
```

- 인덱스 추가는 실제 조회 쿼리 기준으로 판단 (미리 과도하게 걸지 않기)

---

## 9️⃣ 테이블 옵션 규칙

- 모든 테이블에 명시:

```sql
ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
```

- `utf8` 금지 (이모지 🐶 저장 불가 — 커뮤니티 서비스 특성상 필수)

---

## 🔟 컬럼 배치 순서 규칙

어느 테이블을 열어도 구조가 예측 가능하도록 순서를 고정합니다.

```
1. PK
2. FK들
3. 비즈니스 컬럼 (핵심 → 부가 순)
4. 상태/플래그 (status, is_xxx)
5. 도메인 시간 컬럼 (answered_at 등)
6. 감사 컬럼 (created_at → updated_at → deleted_at)
```

---

## 1️⃣1️⃣ 외부 API 필드 처리 규칙

> **DB 컬럼명은 자체 컨벤션을 따르고, 외부 API 필드명과의 매핑은 DTO/매퍼 레이어에서 처리한다.**

- API 필드명을 DB에 그대로 쓰지 않는 것을 원칙으로 함 (`bgnde` → `rescued_date` 등)
- 단, 공공API 연동 테이블(`animal`)은 팀 합의로 API 필드명 유지 가능 — 유지 시 COMMENT에 의미 필수 기재
- API의 타입을 신뢰하지 말 것: 숫자처럼 보여도 자릿수 확인 후 타입 결정

---

## ✅ 리뷰 체크리스트

PR/ERD 리뷰 시 아래를 확인합니다.

```
[ ] 테이블명이 단수형 snake_case인가?
[ ] PK가 단일 BIGINT AUTO_INCREMENT인가?
[ ] 모든 참조 컬럼에 FK 제약이 있는가? (폴리모픽 제외)
[ ] 유일해야 하는 값에 UNIQUE 제약이 있는가?
[ ] 시간 컬럼이 DATETIME(6)인가? TIMESTAMP는 없는가?
[ ] created_at에만 DEFAULT가 있는가?
[ ] ON UPDATE CURRENT_TIMESTAMP가 없는가?
[ ] ENUM 대신 VARCHAR + COMMENT(허용값)를 썼는가?
[ ] 음수 불가 숫자에 UNSIGNED가 있는가?
[ ] utf8mb4 / InnoDB가 명시되어 있는가?
[ ] 컬럼 배치 순서(PK→FK→비즈니스→플래그→시간)를 지켰는가?
[ ] 모든 테이블/컬럼에 COMMENT가 있는가?
```
