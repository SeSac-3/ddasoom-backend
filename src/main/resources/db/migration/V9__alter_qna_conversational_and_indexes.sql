-- =============================================
-- db/migration/V9__alter_qna_conversational_and_indexes.sql
-- ⚠️ answered_at 은 V1의 앱관리 컬럼 그대로 사용 → 건드리지 않음(drop/modify 없음)
-- =============================================

-- (A) 대화형 전환: answer / answered_id 제거 (answered_at 유지)
ALTER TABLE `qna` DROP FOREIGN KEY `fk_qna_answered`;
ALTER TABLE `qna` DROP COLUMN `answered_id`;
ALTER TABLE `qna` DROP COLUMN `answer`;

-- (B) 복합 인덱스 (필터 → 정렬 순, 정렬 방향까지 일치 → filesort 제거)
-- 유저 목록:      WHERE questioner_id=? AND deleted_at IS NULL ORDER BY created_at DESC
CREATE INDEX `idx_qna_questioner_created`
    ON `qna` (`questioner_id`, `deleted_at`, `created_at` DESC);

-- 관리자 상태필터: WHERE status=? AND deleted_at IS NULL ORDER BY created_at DESC
CREATE INDEX `idx_qna_status_created`
    ON `qna` (`status`, `deleted_at`, `created_at` DESC);

-- 코멘트 스레드:   WHERE qna_id=? AND deleted_at IS NULL ORDER BY created_at ASC
CREATE INDEX `idx_qna_comment_qna_created`
    ON `qna_comment` (`qna_id`, `deleted_at`, `created_at`);