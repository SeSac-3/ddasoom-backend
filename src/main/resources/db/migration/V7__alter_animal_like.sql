-- =============================================
-- animal_like: soft delete → 물리 삭제(query delete) 전환
-- =============================================
-- 좋아요 취소는 행을 물리적으로 DELETE 하므로 deleted_at 컬럼이 필요 없음.
-- (DB 컨벤션 §5 "물리 DELETE 금지 / soft delete"의 팀 합의 예외 — animal_like 한정)
--
-- ⚠️ 이 마이그레이션은 seed/V102__seed_animal_like.sql 수정과 반드시 세트로 적용해야 함.
--    Flyway는 폴더가 아니라 버전 번호(V5 < V100 < V102)로 전역 정렬하므로,
--    V5가 deleted_at을 드롭한 뒤 V102가 deleted_at에 INSERT하면 fresh 마이그레이션이 실패함.
--    (PR 문서의 "마이그레이션 주의" 섹션 참고)
--
-- MySQL 8.0은 DROP COLUMN에 IF EXISTS를 지원하지 않으므로 조건 없이 드롭.

ALTER TABLE `animal_like` DROP COLUMN `deleted_at`;
