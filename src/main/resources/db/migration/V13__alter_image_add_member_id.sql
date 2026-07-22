-- =============================================
-- V13__alter_image_add_member_id.sql
-- 임시 이미지(owner_id NULL) 탈취 방지 — 업로더 기록 컬럼 추가
--   imageId 추측으로 타인이 방금 올린 임시 이미지를 자기 게시글에 확정 연결하는 벡터를 차단한다.
--   attach() 확정 연결 시 "업로더 == 요청자" 검증의 근거 데이터.
--   NULL 허용: V13 이전 업로드분은 업로더를 알 수 없어 백필이 불가.
--              단 애플리케이션에서 "업로더 NULL + owner_id NULL"은 연결 거부로 처리해 구멍을 남기지 않는다.
--   owner_id는 폴리모픽이라 FK 예외지만, member_id는 실제 참조이므로 FK 필수 (DB 컨벤션 7장)
-- =============================================

ALTER TABLE `image`
    ADD COLUMN `member_id` BIGINT NULL COMMENT '업로더 회원 식별 FK (NULL = V13 이전 레거시 업로드분)'
    AFTER `owner_id`;

ALTER TABLE `image`
    ADD CONSTRAINT `fk_image_member` FOREIGN KEY (`member_id`) REFERENCES `member` (`member_id`);