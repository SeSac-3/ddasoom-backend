-- V5__alter_image_owner_id_nullable.sql
-- 하이브리드 업로드 플로우 반영: 업로드 시점(소유자 확정 전)의 임시 상태를 owner_id NULL로 표현

ALTER TABLE `image`
    MODIFY `owner_id` BIGINT NULL COMMENT '소유자 논리 참조 (FK 미설정 — 폴리모픽, NULL = 업로드 후 미확정 임시 상태)';