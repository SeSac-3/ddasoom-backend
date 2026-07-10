-- =============================================
-- [개발용 더미] animal_like (local/test 전용)
-- =============================================

-- [좋아요-1] 동물1(뭉치)에 회원 user01(박하늘) 좋아요 - 정상 활성 케이스, 동물1 like_count=2와 합산 일치시키는 첫 건
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (1, 6, '2026-06-03 10:00:00.000000', '2026-06-03 10:00:00.000000', NULL);

-- [좋아요-2] 동물1(뭉치)에 회원 user02(최바다) 좋아요 - 동물1 like_count=2와 합산 일치시키는 두 번째 건 (같은 동물, 다른 회원 다건 검증)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (1, 7, '2026-06-04 11:00:00.000000', '2026-06-04 11:00:00.000000', NULL);

-- [좋아요-3] 동물1(뭉치)에 회원 user04(한별님) 좋아요 후 취소 - 취소된(soft-delete) 좋아요 행이 활성 카운트/목록에서 제외되는지 검증용
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (1, 9, '2026-06-05 08:00:00.000000', '2026-06-06 09:00:00.000000', '2026-06-06 09:00:00.000000');

-- [좋아요-4] 동물2(코리안숏헤어)에 회원 user02(최바다) 좋아요 눌렀다가 취소 - soft-delete(deleted_at 값 있음) 케이스, "취소 후 재좋아요" 시나리오의 대상
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (2, 7, '2026-05-17 10:00:00.000000', '2026-05-18 09:00:00.000000', '2026-05-18 09:00:00.000000');

-- [좋아요-5] 동물3(초코)에 회원 user01(박하늘) 좋아요 - 동일 회원(user01)이 서로 다른 동물에 좋아요 남기는 케이스 (회원 기준 목록 조회 검증)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (3, 6, '2026-04-21 09:00:00.000000', '2026-04-21 09:00:00.000000', NULL);

-- [좋아요-6] 동물7(대박이)에 회원 user01(박하늘) 좋아요 - 동물7 like_count=3 중 일부 (다건 좋아요 분산 검증용 세트의 일부)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (7, 6, '2026-02-16 08:00:00.000000', '2026-02-16 08:00:00.000000', NULL);

-- [좋아요-7] 동물7(대박이)에 회원 user02(최바다) 좋아요 - 동물7 like_count=3 중 일부
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (7, 7, '2026-02-17 08:00:00.000000', '2026-02-17 08:00:00.000000', NULL);

-- [좋아요-8] 동물7(대박이)에 회원 user03(정온유) 좋아요 - 동물7 like_count=3과 실제 활성 행 수를 정확히 일치시키는 세 번째 건
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (7, 8, '2026-02-18 08:00:00.000000', '2026-02-18 08:00:00.000000', NULL);

-- [좋아요-9] 동물9(싱크로)에 회원 user02(최바다) 좋아요 - 실제 활성 좋아요는 이 1건뿐인데 동물9.like_count는 5로 저장되어 있음(캐시 불일치 검증용, 동물-9 코멘트와 짝)
INSERT INTO `animal_like` (`animal_id`, `member_id`, `created_at`, `updated_at`, `deleted_at`)
VALUES (9, 7, '2026-01-22 09:00:00.000000', '2026-01-22 09:00:00.000000', NULL);