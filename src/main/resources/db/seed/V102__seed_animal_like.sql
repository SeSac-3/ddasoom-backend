-- =============================================================
-- V102__seed_animal_like.sql  [개발용 더미 — 전면 리빌드 2026-07]
-- 로컬/테스트 전용. V100(회원)·V101(동물) 이후 실행 전제.
--
-- 목표:
--  * 명시 동물(뭉치·나비 등 TOP 후보)에 좋아요를 몰아줘 인기 정렬이 유의미하게
--  * PK 하드코딩 대신 이메일/유기번호 기준 조인 — 시드 순서·행 수가 바뀌어도 안전
--  * 마지막에 like_count 캐시 컬럼을 실제 활성 행 수로 일괄 동기화 (정합 보장)
--  * animal_like는 V7에서 deleted_at 드롭 — "취소" 케이스 없음(행 삭제 방식)
-- =============================================================

-- ── 1. 명시 동물에 좋아요 몰아주기 — bulk 회원 일부가 인기 동물에 좋아요 ──
-- 뭉치 12명 / 나비 10명 / 초코 8명 / 보리 6명 / 치즈 5명 (명시 순위 형성)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
SELECT a.animal_id, m.member_id,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY)
FROM (SELECT animal_id, abandonment_id FROM animal WHERE abandonment_id = '413587202600162') a
JOIN (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email LIKE 'bulk%' ) m ON m.rn <= 12;

INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
SELECT a.animal_id, m.member_id,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY)
FROM (SELECT animal_id FROM animal WHERE abandonment_id = '469569202600557') a
JOIN (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email LIKE 'bulk%' ) m ON m.rn BETWEEN 13 AND 22;

INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
SELECT a.animal_id, m.member_id,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY)
FROM (SELECT animal_id FROM animal WHERE abandonment_id = '450650202601690') a
JOIN (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email LIKE 'bulk%' ) m ON m.rn BETWEEN 23 AND 30;

INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
SELECT a.animal_id, m.member_id,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY)
FROM (SELECT animal_id FROM animal WHERE abandonment_id = '441323202600881') a
JOIN (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email LIKE 'bulk%' ) m ON m.rn BETWEEN 31 AND 36;

INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
SELECT a.animal_id, m.member_id,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL m.rn DAY)
FROM (SELECT animal_id FROM animal WHERE abandonment_id = '427701202600233') a
JOIN (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email LIKE 'bulk%' ) m ON m.rn BETWEEN 37 AND 41;

-- ── 2. 흩뿌리기 — 명시 유저 5명이 대량 동물에 1건씩 (마이페이지 "내 좋아요 목록" 검증) ──
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
SELECT a.animal_id, m.member_id,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL a.rn DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL a.rn DAY)
FROM (SELECT animal_id, ROW_NUMBER() OVER (ORDER BY animal_id) AS rn
      FROM animal WHERE nickname LIKE '더미동물%') a
JOIN (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email IN ('user01@ddasoom.com','user02@ddasoom.com','user03@ddasoom.com',
                                   'user04@ddasoom.com','user05@ddasoom.com')) m
  ON a.rn = m.rn * 4;   -- 4·8·12·16·20번째 더미동물에 1건씩

-- ── 3. like_count 캐시 동기화 — 실제 행 수와 일치 보장 (배치 동기화와 동일 결과) ──
UPDATE `animal` a
SET a.like_count = (SELECT COUNT(*) FROM animal_like al WHERE al.animal_id = a.animal_id);
