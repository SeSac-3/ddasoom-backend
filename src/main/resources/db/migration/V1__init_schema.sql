-- =============================================================
-- V1__init_schema.sql
-- 따숨(Ddasoom) 초기 스키마 (13개 테이블)
-- 기준: ERD 2026-07-07 + 검토 문서 결정 사항(3-1 A / 3-2 C / 3-3 B / 3-4 C(>0) / 3-5 반영)
--       + 테이블명 단수형 (DB 컨벤션 2026-07-07 개정 반영)
-- CREATE 순서: FK 의존성 순 (member → animal → post/qna → 자식 테이블)
-- =============================================================

-- =============================================
-- 1. member
-- =============================================
CREATE TABLE `member` (
                          `member_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '회원 식별 PK',
                          `email`      VARCHAR(255) NOT NULL COMMENT '로그인 ID',
                          `password`   VARCHAR(255) NULL     COMMENT 'SNS 가입자는 NULL, BCrypt 암호화 저장',
                          `name`       VARCHAR(50)  NULL     COMMENT '회원 실명',
                          `nickname`   VARCHAR(20)  NULL     COMMENT '회원 닉네임',
                          `tel`        VARCHAR(20)  NULL     COMMENT '회원 휴대전화 번호',
                          `role`       VARCHAR(20)  NOT NULL DEFAULT 'GUEST' COMMENT 'ADMIN / USER / GUEST',
                          `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '회원가입 일시',
                          `updated_at` DATETIME(6)  NOT NULL COMMENT '회원 정보 수정 일시',
                          `deleted_at` DATETIME(6)  NULL     COMMENT '삭제 일시 (NULL = 활성)',
                          CONSTRAINT `pk_member` PRIMARY KEY (`member_id`),
                          CONSTRAINT `uk_member_email` UNIQUE (`email`),
                          CONSTRAINT `uk_member_nickname` UNIQUE (`nickname`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='회원';

-- =============================================
-- 2. member_social
-- =============================================
CREATE TABLE `member_social` (
                                 `member_social_id` BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'SNS회원 식별 PK',
                                 `member_id`        BIGINT       NOT NULL COMMENT '회원 식별 FK',
                                 `provider`         VARCHAR(20)  NOT NULL COMMENT '소셜 제공자 (KAKAO / NAVER / GOOGLE)',
                                 `provider_id`      VARCHAR(255) NOT NULL COMMENT '소셜 플랫폼에서 발급한 해당 유저의 고유 식별자',
                                 `created_at`       DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '연동 일시',
                                 `updated_at`       DATETIME(6)  NOT NULL COMMENT '수정 일시',
                                 CONSTRAINT `pk_member_social` PRIMARY KEY (`member_social_id`),
                                 CONSTRAINT `uk_member_social_provider` UNIQUE (`provider`, `provider_id`),
                                 CONSTRAINT `fk_member_social_member` FOREIGN KEY (`member_id`)
                                     REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='SNS 연동 회원';

-- =============================================
-- 3. login_log (append-only 로그 — updated_at/deleted_at 없음)
-- =============================================
CREATE TABLE `login_log` (
                             `login_log_id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '로그인 이력 식별 PK',
                             `member_id`    BIGINT      NOT NULL COMMENT '회원 식별 FK',
                             `login_type`   VARCHAR(20) NOT NULL COMMENT 'LOCAL / KAKAO / GOOGLE / NAVER',
                             `created_at`   DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '로그인 일시',
                             CONSTRAINT `pk_login_log` PRIMARY KEY (`login_log_id`),
                             CONSTRAINT `fk_login_log_member` FOREIGN KEY (`member_id`)
                                 REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='로그인 이력';

-- =============================================
-- 4. animal
--    ※ weight: 결정 3-4(C안) — DECIMAL(5,2) + CHECK (> 0)
--    ※ abandonment_id: 컨벤션 11항 — 외부 API 숫자형 문자열 ID는 VARCHAR 저장
-- =============================================
CREATE TABLE `animal` (
                          `animal_id`       BIGINT            NOT NULL AUTO_INCREMENT COMMENT '유기동물 식별 PK',
                          `abandonment_id`  VARCHAR(20)       NOT NULL COMMENT '유기번호 (보호소 관리 원본 문자열, 외부 API 값 그대로)',
                          `kind`            VARCHAR(20)       NOT NULL COMMENT '상위 품종 분류 (개/고양이 등)',
                          `nickname`        VARCHAR(50)       NOT NULL DEFAULT '미정' COMMENT '유기동물 닉네임',
                          `gender`          CHAR(1)           NOT NULL DEFAULT 'Q' COMMENT '성별 (M / F / Q(미상))',
                          `type_name`       VARCHAR(50)       NOT NULL COMMENT '품종 이름 (예: 말티즈)',
                          `age`             SMALLINT UNSIGNED NOT NULL COMMENT '출생 연도 (예: 2026)',
                          `location`        VARCHAR(100)      NOT NULL COMMENT '위치 (시군구)',
                          `weight`          DECIMAL(5,2)      NOT NULL COMMENT '몸무게 (kg)',
                          `color`           VARCHAR(50)       NOT NULL COMMENT '색깔',
                          `special_mark`    VARCHAR(255)      NULL     DEFAULT '없음' COMMENT '특이사항',
                          `vaccination_chk` VARCHAR(50)       NULL     DEFAULT '접종 안함' COMMENT '백신 접종 유무',
                          `image_url`       VARCHAR(255)      NULL     COMMENT '이미지 URL',
                          `like_count`      INT UNSIGNED      NOT NULL DEFAULT 0 COMMENT '좋아요 수 (캐시 컬럼, animal_like 기준 동기화)',
                          `is_fostered`     BOOLEAN           NOT NULL DEFAULT FALSE COMMENT '임시보호 여부 (foster 기준 동기화)',
                          `rescued_at`      DATETIME(6)       NULL     COMMENT '구조 일시 (서비스 로직에서 명시적 세팅, 자동화 금지)',
                          `created_at`      DATETIME(6)       NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
                          `updated_at`      DATETIME(6)       NOT NULL COMMENT '수정 일시',
                          `deleted_at`      DATETIME(6)       NULL     COMMENT '삭제 일시 (NULL = 활성)',
                          CONSTRAINT `pk_animal` PRIMARY KEY (`animal_id`),
                          CONSTRAINT `uk_animal_abandonment` UNIQUE (`abandonment_id`),
                          CONSTRAINT `chk_animal_weight` CHECK (`weight` > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='유기동물';

-- =============================================
-- 5. animal_like (단일 PK + 복합 UNIQUE — 컨벤션 7항)
-- =============================================
CREATE TABLE `animal_like` (
                               `animal_like_id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT '유기동물 좋아요 PK',
                               `animal_id`      BIGINT      NOT NULL COMMENT '유기동물 식별 FK',
                               `member_id`      BIGINT      NOT NULL COMMENT '회원 식별 FK',
                               `created_at`     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시 (좋아요 누른 시각)',
                               `updated_at`     DATETIME(6) NOT NULL COMMENT '수정 일시',
                               `deleted_at`     DATETIME(6) NULL     COMMENT '삭제 일시 (NULL = 활성, 좋아요 취소 시 세팅)',
                               CONSTRAINT `pk_animal_like` PRIMARY KEY (`animal_like_id`),
                               CONSTRAINT `uk_animal_like_animal_member` UNIQUE (`animal_id`, `member_id`),
                               CONSTRAINT `fk_animal_like_animal` FOREIGN KEY (`animal_id`)
                                   REFERENCES `animal` (`animal_id`) ON DELETE RESTRICT,
                               CONSTRAINT `fk_animal_like_member` FOREIGN KEY (`member_id`)
                                   REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='유기동물 좋아요';

-- =============================================
-- 6. foster (user_id: 결정 3-2 — 팀 합의 명칭 유지)
-- =============================================
CREATE TABLE `foster` (
                          `foster_id`          BIGINT      NOT NULL AUTO_INCREMENT COMMENT '임시보호 식별 PK',
                          `animal_id`          BIGINT      NOT NULL COMMENT '유기동물 식별 FK (animal)',
                          `user_id`            BIGINT      NOT NULL COMMENT '신청 회원 식별 FK (member)',
                          `reviewer_id`        BIGINT      NULL     COMMENT '관리자 식별 FK (member)',
                          `foster_num`         CHAR(36)    NOT NULL COMMENT '글번호 식별 UUID',
                          `age`                VARCHAR(10) NOT NULL COMMENT '신청자 나이',
                          `job`                VARCHAR(30) NOT NULL COMMENT '신청자 직업',
                          `message`            TEXT        NULL     COMMENT '신청자 비고',
                          `answer`             TEXT        NULL     COMMENT '관리자 답변',
                          `status`             VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / REJECTED / FOSTERING / EXTENDED / ENDED',
                          `foster_start_at`    DATETIME(6) NULL     COMMENT '임시보호 시작일',
                          `foster_end_at`      DATETIME(6) NULL     COMMENT '임시보호 종료일',
                          `foster_extend_at`   DATETIME(6) NULL     COMMENT '임시보호 연장일',
                          `foster_complete_at` DATETIME(6) NULL     COMMENT '임시보호 최종 종료일',
                          `created_at`         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '임시보호신청 작성일',
                          `updated_at`         DATETIME(6) NOT NULL COMMENT '임시보호신청 수정일',
                          `deleted_at`         DATETIME(6) NULL     COMMENT '임시보호신청 삭제일 (NULL = 활성)',
                          CONSTRAINT `pk_foster` PRIMARY KEY (`foster_id`),
                          CONSTRAINT `uk_foster_num` UNIQUE (`foster_num`),
                          CONSTRAINT `fk_foster_animal` FOREIGN KEY (`animal_id`)
                              REFERENCES `animal` (`animal_id`) ON DELETE RESTRICT,
                          CONSTRAINT `fk_foster_user` FOREIGN KEY (`user_id`)
                              REFERENCES `member` (`member_id`) ON DELETE RESTRICT,
                          CONSTRAINT `fk_foster_reviewer` FOREIGN KEY (`reviewer_id`)
                              REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='임시보호 신청';

-- =============================================
-- 7. post
-- =============================================
CREATE TABLE `post` (
                        `post_id`       BIGINT       NOT NULL AUTO_INCREMENT COMMENT '게시글 식별 PK',
                        `member_id`     BIGINT       NOT NULL COMMENT '작성자 회원 식별 FK',
                        `board_type`    VARCHAR(30)  NOT NULL COMMENT 'ADOPTION_REVIEW / PET_INFO',
                        `category`      VARCHAR(50)  NOT NULL COMMENT '보드별 카테고리 (강아지, 고양이...)',
                        `title`         VARCHAR(255) NOT NULL COMMENT '게시글 제목',
                        `content`       TEXT         NOT NULL COMMENT '게시글 본문',
                        `view_count`    INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '조회수',
                        `comment_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '댓글수',
                        `created_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
                        `updated_at`    DATETIME(6)  NOT NULL COMMENT '수정 일시',
                        `deleted_at`    DATETIME(6)  NULL     COMMENT '삭제 일시 (NULL = 활성)',
                        CONSTRAINT `pk_post` PRIMARY KEY (`post_id`),
                        CONSTRAINT `fk_post_member` FOREIGN KEY (`member_id`)
                            REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글';

-- =============================================
-- 8. post_comment
-- =============================================
CREATE TABLE `post_comment` (
                                `post_comment_id`   BIGINT      NOT NULL AUTO_INCREMENT COMMENT '댓글 식별 PK',
                                `post_id`           BIGINT      NOT NULL COMMENT '게시글 식별 FK',
                                `member_id`         BIGINT      NOT NULL COMMENT '작성자 회원 식별 FK',
                                `parent_comment_id` BIGINT      NULL     COMMENT '부모 댓글 FK (일반 댓글은 NULL)',
                                `content`           TEXT        NOT NULL COMMENT '댓글 본문',
                                `created_at`        DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 일시',
                                `updated_at`        DATETIME(6) NOT NULL COMMENT '수정 일시',
                                `deleted_at`        DATETIME(6) NULL     COMMENT '삭제 일시 (NULL = 활성)',
                                CONSTRAINT `pk_post_comment` PRIMARY KEY (`post_comment_id`),
                                CONSTRAINT `fk_post_comment_post` FOREIGN KEY (`post_id`)
                                    REFERENCES `post` (`post_id`) ON DELETE RESTRICT,
                                CONSTRAINT `fk_post_comment_member` FOREIGN KEY (`member_id`)
                                    REFERENCES `member` (`member_id`) ON DELETE RESTRICT,
                                CONSTRAINT `fk_post_comment_parent` FOREIGN KEY (`parent_comment_id`)
                                    REFERENCES `post_comment` (`post_comment_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='게시글 댓글';

-- =============================================
-- 9. qna (answered_id / answered_at: 결정 3-1 — NULL 허용)
-- =============================================
CREATE TABLE `qna` (
                       `qna_id`        BIGINT       NOT NULL AUTO_INCREMENT COMMENT '문의 식별 PK',
                       `questioner_id` BIGINT       NOT NULL COMMENT '문의자 회원 식별 FK',
                       `answered_id`   BIGINT       NULL     COMMENT '답변 관리자 식별 FK (답변 전 NULL)',
                       `title`         VARCHAR(255) NOT NULL COMMENT 'QNA 문의 제목',
                       `content`       TEXT         NOT NULL COMMENT 'QNA 문의 내용',
                       `answer`        TEXT         NULL     COMMENT '관리자 답변',
                       `status`        VARCHAR(30)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / ANSWERED',
                       `is_visible`    BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '공개 여부',
                       `answered_at`   DATETIME(6)  NULL     COMMENT '답변 완료일 (답변 전 NULL)',
                       `created_at`    DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '문의 작성일',
                       `updated_at`    DATETIME(6)  NOT NULL COMMENT '수정일 (사용자, 관리자 통합)',
                       `deleted_at`    DATETIME(6)  NULL     COMMENT '삭제일 (NULL = 활성)',
                       CONSTRAINT `pk_qna` PRIMARY KEY (`qna_id`),
                       CONSTRAINT `fk_qna_questioner` FOREIGN KEY (`questioner_id`)
                           REFERENCES `member` (`member_id`) ON DELETE RESTRICT,
                       CONSTRAINT `fk_qna_answered` FOREIGN KEY (`answered_id`)
                           REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='1:1 문의';

-- =============================================
-- 10. qna_comment (member_id FK — 컨벤션 7항에 따라 추가)
-- =============================================
CREATE TABLE `qna_comment` (
                               `qna_comment_id` BIGINT      NOT NULL AUTO_INCREMENT COMMENT 'Q&A 댓글 식별 PK',
                               `qna_id`         BIGINT      NOT NULL COMMENT '문의 식별 FK',
                               `member_id`      BIGINT      NOT NULL COMMENT '댓글 작성자 회원 식별 FK',
                               `content`        TEXT        NOT NULL COMMENT '댓글(답변) 본문 내용',
                               `created_at`     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '댓글 작성일',
                               `updated_at`     DATETIME(6) NOT NULL COMMENT '댓글 수정일',
                               `deleted_at`     DATETIME(6) NULL     COMMENT '댓글 삭제일 (NULL = 활성)',
                               CONSTRAINT `pk_qna_comment` PRIMARY KEY (`qna_comment_id`),
                               CONSTRAINT `fk_qna_comment_qna` FOREIGN KEY (`qna_id`)
                                   REFERENCES `qna` (`qna_id`) ON DELETE RESTRICT,
                               CONSTRAINT `fk_qna_comment_member` FOREIGN KEY (`member_id`)
                                   REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='Q&A 댓글';

-- =============================================
-- 11. notice
-- =============================================
CREATE TABLE `notice` (
                          `notice_id`  BIGINT       NOT NULL AUTO_INCREMENT COMMENT '공지사항 식별 PK',
                          `member_id`  BIGINT       NOT NULL COMMENT '작성자 회원 식별 FK',
                          `title`      VARCHAR(255) NOT NULL COMMENT '공지사항 제목',
                          `content`    TEXT         NOT NULL COMMENT '공지사항 내용',
                          `is_visible` BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '공개 여부',
                          `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '공지사항 작성일',
                          `updated_at` DATETIME(6)  NOT NULL COMMENT '공지사항 수정일',
                          `deleted_at` DATETIME(6)  NULL     COMMENT '공지사항 삭제일 (NULL = 활성)',
                          CONSTRAINT `pk_notice` PRIMARY KEY (`notice_id`),
                          CONSTRAINT `fk_notice_member` FOREIGN KEY (`member_id`)
                              REFERENCES `member` (`member_id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='공지사항';

-- =============================================
-- 12. faq
-- =============================================
CREATE TABLE `faq` (
                       `faq_id`     BIGINT       NOT NULL AUTO_INCREMENT COMMENT 'FAQ 식별 PK',
                       `category`   VARCHAR(50)  NOT NULL COMMENT '입양절차, 서비스이용 등',
                       `question`   VARCHAR(255) NOT NULL COMMENT 'FAQ 질문',
                       `answer`     TEXT         NOT NULL COMMENT 'FAQ 답변',
                       `is_visible` BOOLEAN      NOT NULL DEFAULT TRUE COMMENT '공개 여부',
                       `created_at` DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT 'FAQ 작성일',
                       `updated_at` DATETIME(6)  NOT NULL COMMENT 'FAQ 수정일',
                       `deleted_at` DATETIME(6)  NULL     COMMENT 'FAQ 삭제일 (NULL = 활성)',
                       CONSTRAINT `pk_faq` PRIMARY KEY (`faq_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='FAQ';

-- =============================================
-- 13. image (폴리모픽 — FK 없음, 수동 인덱스 필수)
-- =============================================
CREATE TABLE `image` (
                         `image_id`           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '이미지 식별 PK',
                         `owner_type`         VARCHAR(30)     NOT NULL COMMENT 'POST / QNA / NOTICE / ANIMAL ...',
                         `owner_id`           BIGINT          NOT NULL COMMENT '논리적 참조 (FK 미설정 — 폴리모픽)',
                         `image_key`          VARCHAR(255)    NOT NULL COMMENT '{yyyy}/{MM}/{용도}/{uuid}.{확장자}',
                         `original_file_name` VARCHAR(255)    NOT NULL COMMENT '업로드 원본 파일명',
                         `file_size`          BIGINT UNSIGNED NOT NULL COMMENT '파일 크기 (byte)',
                         `mime_type`          VARCHAR(100)    NOT NULL COMMENT 'image/jpeg 등',
                         `image_order`        INT UNSIGNED    NOT NULL DEFAULT 0 COMMENT '노출 순서',
                         `is_thumbnail`       BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '대표 이미지 여부',
                         `created_at`         DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '업로드 일시',
                         `updated_at`         DATETIME(6)     NOT NULL COMMENT '수정 일시',
                         `deleted_at`         DATETIME(6)     NULL     COMMENT '삭제 일시 (NULL = 활성)',
                         CONSTRAINT `pk_image` PRIMARY KEY (`image_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='이미지 (폴리모픽)';

CREATE INDEX `idx_image_owner` ON `image` (`owner_type`, `owner_id`);