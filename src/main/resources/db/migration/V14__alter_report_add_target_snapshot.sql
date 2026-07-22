-- =============================================
-- V14__alter_report_add_target_snapshot.sql
-- 신고 대상 스냅샷 컬럼 추가 (+ 피신고자 FK)
--   승인 = 대상 soft delete/탈퇴이므로, 처리 후에는 target_id로 대상을 복원할 수 없다.
--   접수 시점의 소유자·제목·본문 발췌를 report에 고정 저장해 처리 이력에서 대상을 보존한다.
-- =============================================

ALTER TABLE `report`
    ADD COLUMN `reported_member_id` BIGINT       NULL COMMENT '피신고자 (대상 소유자, member FK)',
    ADD COLUMN `target_parent_id`   BIGINT       NULL COMMENT '댓글 신고 시 원 게시글 PK',
    ADD COLUMN `target_title`       VARCHAR(255) NULL COMMENT '대상 제목 스냅샷',
    ADD COLUMN `target_snippet`     VARCHAR(500) NULL COMMENT '대상 본문 발췌 스냅샷';

-- reported_member_id를 NULL 허용으로 둔 이유: 스냅샷 도입(V14) 이전 데이터 + 접수 시 소유자 확보 실패 케이스.
-- member는 soft delete(deleted_at)라 강제탈퇴 후에도 row가 남는다 → 피신고자를 탈퇴시켜도 이 FK 무결성이 유지된다.
ALTER TABLE `report`
    ADD CONSTRAINT `fk_report_reported_member`
        FOREIGN KEY (`reported_member_id`) REFERENCES `member` (`member_id`) ON DELETE RESTRICT;

-- 회원별 신고 이력 조회(관리자 회원 상세)용.
-- idx_report_status_created와 동일한 filter → filter → sort 컬럼 순서.
CREATE INDEX `idx_report_reported_member` ON `report` (`reported_member_id`, `deleted_at`, `created_at`);
