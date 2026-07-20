-- =============================================
-- V12__alter_member_add_status.sql
-- 회원 노출 상태 컬럼 추가 (신고 제재 대응)
--   VARCHAR (EnumType.STRING) — ACTIVE(정상) / HIDDEN(신고 제재 숨김)
--   신고 접수 → 관리자가 관리자페이지에서 직접 확인 후 수동 전환 (자동 처리 없음)
--   ※ ENUM 타입 금지 컨벤션(DB 6장) 준수 — 값 검증은 Java Enum이 담당
-- =============================================

ALTER TABLE `member`
    ADD COLUMN `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE'
    COMMENT '회원 노출 상태 (ACTIVE=정상, HIDDEN=신고 제재 숨김)'
    AFTER `event_status`;