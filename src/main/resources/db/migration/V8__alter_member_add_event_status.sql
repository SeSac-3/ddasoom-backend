-- =============================================
-- V8__add_member_event_status.sql
-- 공지사항 이벤트 참여 상태 컬럼 추가
--   VARCHAR (EnumType.STRING) — NONE(미참여) / PARTICIPATING(참여중)
--   board 도메인이 유저별 게시글 작성 수를 기준으로 상태를 전이시킨다.
--   ※ 이벤트가 상시 기능이 아니게 되면 별도 event 테이블로 분리 검토 (현 단계는 Member 컬럼으로 충분)
-- =============================================

ALTER TABLE `member`
    ADD COLUMN `event_status` VARCHAR(20) NOT NULL DEFAULT 'NONE'
    COMMENT '공지 이벤트 참여 상태 (NONE=미참여, PARTICIPATING=참여중)'
    AFTER `role`;