-- =============================================
-- db/migration/V11__create_report_table.sql
-- 신고기능 추가 -> 해당 테이블 추가
-- =============================================

CREATE TABLE `report` (
    `report_id`    BIGINT       NOT NULL AUTO_INCREMENT COMMENT '신고 PK',
    `reporter_id`  BIGINT       NOT NULL COMMENT '신고자 (member FK)',
    `target_type`  VARCHAR(20)  NOT NULL COMMENT 'POST / POST_COMMENT / MEMBER',
    `target_id`    BIGINT       NOT NULL COMMENT '신고 대상 PK (폴리모픽 논리 참조, FK 없음)',
    `reason`       VARCHAR(30)  NOT NULL COMMENT 'SPAM / ABUSE / INAPPROPRIATE / FRAUD / ETC',
    `content`      TEXT         NULL COMMENT '상세 사유 (ETC일 때 필수 — 앱 검증)',
    `status`       VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING / APPROVED / REJECTED',
    `processed_id` BIGINT       NULL COMMENT '처리한 관리자 (member FK, 역할기반 이름)',
    `processed_at` DATETIME(6)  NULL COMMENT '처리 시각 (앱이 세팅)',
    `created_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '생성 시각',
    `updated_at`   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '수정 시각',
    `deleted_at`   DATETIME(6)  NULL COMMENT 'soft delete (NULL = 활성)',
    PRIMARY KEY (`report_id`),
    CONSTRAINT `fk_report_reporter`  FOREIGN KEY (`reporter_id`)  REFERENCES `member` (`member_id`) ON DELETE RESTRICT,
    CONSTRAINT `fk_report_processed` FOREIGN KEY (`processed_id`) REFERENCES `member` (`member_id`) ON DELETE RESTRICT,
    CONSTRAINT `uk_report_reporter_target` UNIQUE (`reporter_id`, `target_type`, `target_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='신고';

CREATE INDEX `idx_report_target` ON `report` (`target_type`, `target_id`);
CREATE INDEX `idx_report_status_created` ON `report` (`status`, `deleted_at`, `created_at`);