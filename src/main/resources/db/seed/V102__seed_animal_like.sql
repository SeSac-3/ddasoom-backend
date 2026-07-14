-- =============================================
-- [개발용 더미] animal_like (local/test 전용)
-- =============================================
-- 물리 삭제 전환에 따라 deleted_at 컬럼 제거.
-- 기존 [좋아요-3](1,9), [좋아요-4](2,7) 는 "취소(soft-delete)" 케이스였으나,
-- 물리 삭제에서는 취소 = 행이 존재하지 않음 → 시드에서 완전히 제거함.
-- ("취소 후 재좋아요" 시나리오는 이제 '행이 없다 → 새 INSERT'로 자연 검증됨)

-- [좋아요-1] 동물1(뭉치)에 회원 user01(박하늘) 좋아요 - 정상 활성 케이스, 동물1 like_count=2 합산 첫 건
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (1, 6, '2026-06-03 10:00:00.000000', '2026-06-03 10:00:00.000000');

-- [좋아요-2] 동물1(뭉치)에 회원 user02(최바다) 좋아요 - 동물1 like_count=2 합산 두 번째 건 (같은 동물, 다른 회원 다건 검증)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (1, 7, '2026-06-04 11:00:00.000000', '2026-06-04 11:00:00.000000');

-- [좋아요-5] 동물3(초코)에 회원 user01(박하늘) 좋아요 - 동일 회원이 서로 다른 동물에 좋아요 (회원 기준 목록 조회 검증)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (3, 6, '2026-04-21 09:00:00.000000', '2026-04-21 09:00:00.000000');

-- [좋아요-6] 동물7(대박이)에 회원 user01(박하늘) 좋아요 - 동물7 like_count=3 중 일부
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (7, 6, '2026-02-16 08:00:00.000000', '2026-02-16 08:00:00.000000');

-- [좋아요-7] 동물7(대박이)에 회원 user02(최바다) 좋아요 - 동물7 like_count=3 중 일부
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (7, 7, '2026-02-17 08:00:00.000000', '2026-02-17 08:00:00.000000');

-- [좋아요-8] 동물7(대박이)에 회원 user03(정온유) 좋아요 - 동물7 like_count=3과 활성 행 수 정확히 일치시키는 세 번째 건
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (7, 8, '2026-02-18 08:00:00.000000', '2026-02-18 08:00:00.000000');

-- [좋아요-9] 동물9(싱크로)에 회원 user02(최바다) 좋아요 - 활성 좋아요는 1건인데 animal.like_count=5로 저장됨
--            (캐시 불일치 검증용. 배치 flush가 돌면 상관 서브쿼리 재계산으로 1로 self-heal 되는 것도 함께 확인 가능)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`)
VALUES (9, 7, '2026-01-22 09:00:00.000000', '2026-01-22 09:00:00.000000');
