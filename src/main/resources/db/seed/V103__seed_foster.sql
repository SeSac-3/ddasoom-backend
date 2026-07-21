-- =============================================================
-- V103__seed_foster.sql  [개발용 더미 — 전면 리빌드 2026-07]
-- 로컬/테스트 전용. V100(회원)·V101(동물) 이후 실행 전제.
--
-- 대시보드/통계 검증 목표 (핵심 시드 — 숫자가 예측 가능해야 함):
--  * 만료임박(명시 행으로만 구성 → 카드 숫자 고정 검증):
--      긴급(D-0~D-7)  = 3건  (D-2, D-5, D-7)
--      예정(D-8~D-30) = 4건  (D-10, D-15, D-22, D-29)
--    → bulk의 진행중 건은 종료일을 D+40 이후로 밀어 임박 구간에 안 걸리게 함
--  * 심사대기: 명시 4건 + bulk PENDING → 처리대기 카드
--  * 상태 5종 분포 막대: PENDING / REJECTED / FOSTERING / EXTENDED / ENDED 전부 존재
--  * 월별 추이: created_at을 최근 12개월에 분산 (올해 1~이번달 + 작년 잔여월 — 연도 드롭다운 둘 다 데이터)
--  * 승인율: (FOSTERING+EXTENDED+ENDED) / (+REJECTED) — 분모에서 PENDING 제외 확인
--  * 평균 지속기간: ENDED/진행중 건의 start~end 20~75일 분산
--  * TOP10: 명시 동물 10마리에 신청 몰아주기 (뭉치 9건 > 나비 8건 > … 순위 형성)
-- =============================================================

-- ── 1. 만료임박 명시 7건 (전부 진행 중 상태 — 카드 숫자의 유일한 소스) ──────
-- 긴급 3건: 종료일 D-2 / D-5 / D-7
INSERT INTO `foster` (`animal_id`, `user_id`, `reviewer_id`, `foster_num`, `age`, `job`, `message`, `answer`, `status`,
                      `foster_start_at`, `foster_end_at`, `foster_extend_at`, `foster_complete_at`, `created_at`, `updated_at`) VALUES
    ((SELECT animal_id FROM animal WHERE abandonment_id = '450650202601690'),
     (SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '32', '회사원', '초코 임시보호 중입니다. 연장 여부 곧 결정할게요.', '승인합니다.', 'FOSTERING',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 28 DAY), DATE_ADD(CURDATE(), INTERVAL 2 DAY), NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 28 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '441323202600881'),
     (SELECT member_id FROM member WHERE email = 'user04@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '41', '자영업', '보리 산책 매일 시키고 있어요.', '승인합니다.', 'FOSTERING',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY), DATE_ADD(CURDATE(), INTERVAL 5 DAY), NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 27 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '448290202600412'),
     (SELECT member_id FROM member WHERE email = 'user05@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '27', '프리랜서', '까망이 적응 잘하고 있습니다.', '승인합니다.', 'EXTENDED',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 55 DAY), DATE_ADD(CURDATE(), INTERVAL 7 DAY),
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY), NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 57 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY));

-- 예정 4건: 종료일 D-10 / D-15 / D-22 / D-29
INSERT INTO `foster` (`animal_id`, `user_id`, `reviewer_id`, `foster_num`, `age`, `job`, `message`, `answer`, `status`,
                      `foster_start_at`, `foster_end_at`, `foster_extend_at`, `foster_complete_at`, `created_at`, `updated_at`) VALUES
    ((SELECT animal_id FROM animal WHERE abandonment_id = '452117202600790'),
     (SELECT member_id FROM member WHERE email = 'user06@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '35', '교사', '탄이 대형견이지만 마당 있어서 괜찮아요.', '승인합니다.', 'FOSTERING',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY), DATE_ADD(CURDATE(), INTERVAL 10 DAY), NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 22 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '413587202600162'),
     (SELECT member_id FROM member WHERE email = 'user07@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '29', '간호사', '뭉치 임보 신청합니다. 소형견 경험 있어요.', '승인합니다.', 'FOSTERING',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 15 DAY), DATE_ADD(CURDATE(), INTERVAL 15 DAY), NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 17 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 15 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '435812202601104'),
     (SELECT member_id FROM member WHERE email = 'user08@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '38', '디자이너', '해피 다리 재활 도울 수 있습니다.', '승인합니다.', 'EXTENDED',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 50 DAY), DATE_ADD(CURDATE(), INTERVAL 22 DAY),
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY), NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 52 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '439004202601377'),
     (SELECT member_id FROM member WHERE email = 'user09@ddasoom.com'),
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     UUID(), '31', '개발자', '구름이 장모종 관리 자신 있습니다.', '승인합니다.', 'FOSTERING',
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY), DATE_ADD(CURDATE(), INTERVAL 29 DAY), NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 12 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY));

-- ── 2. 심사대기 명시 4건 (처리대기 카드 + 관리자 심사 시연) ─────────────────
INSERT INTO `foster` (`animal_id`, `user_id`, `reviewer_id`, `foster_num`, `age`, `job`, `message`, `answer`, `status`,
                      `foster_start_at`, `foster_end_at`, `foster_extend_at`, `foster_complete_at`, `created_at`, `updated_at`) VALUES
    ((SELECT animal_id FROM animal WHERE abandonment_id = '469569202600557'),
     (SELECT member_id FROM member WHERE email = 'user10@ddasoom.com'), NULL,
     UUID(), '26', '대학원생', '나비 임보 신청합니다. 고양이 두 마리 키워봤어요.', NULL, 'PENDING',
     NULL, NULL, NULL, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '427701202600233'),
     (SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'), NULL,
     UUID(), '33', '회사원', '치즈 임보 희망합니다. 재택근무라 시간 충분해요.', NULL, 'PENDING',
     NULL, NULL, NULL, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '444561202600925'),
     (SELECT member_id FROM member WHERE email = 'user02@ddasoom.com'), NULL,
     UUID(), '30', '약사', '밤톨이 분리불안 훈련 경험 있습니다.', NULL, 'PENDING',
     NULL, NULL, NULL, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY)),
    ((SELECT animal_id FROM animal WHERE abandonment_id = '413587202600162'),
     (SELECT member_id FROM member WHERE email = 'today01@ddasoom.com'), NULL,
     UUID(), '28', '회사원', '오늘 가입하고 바로 신청합니다!', NULL, 'PENDING',
     NULL, NULL, NULL, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- ── 3. 대량 90건 — 월별 추이·승인율·평균 지속기간·TOP10용 ───────────────────
-- created_at: n MOD 12 개월 전 (+일 지터) → 최근 12개월 전 구간에 고르게
-- status:  n MOD 10 → 0    PENDING (9건)
--                     1    REJECTED (9건)
--                     2~4  ENDED (27건)         : start~end 과거, 기간 20~75일
--                     5~7  FOSTERING (27건)     : end = D+40~+96 (임박 구간 회피)
--                     8~9  EXTENDED (18건)      : end = D+45~+80 (임박 구간 회피)
-- 동물: TOP10 순위 형성을 위해 n MOD 12 < 10 이면 명시 동물 1~10에 배정(내림 가중), 나머지는 더미동물
INSERT INTO `foster` (`animal_id`, `user_id`, `reviewer_id`, `foster_num`, `age`, `job`, `message`, `answer`, `status`,
                      `foster_start_at`, `foster_end_at`, `foster_extend_at`, `foster_complete_at`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 90
)
SELECT
    -- 동물 배정: n<=45는 명시 10마리에 삼각 가중(뭉치가 최다), n>45는 더미동물 순환
    CASE
        WHEN n <= 45 THEN (SELECT MIN(animal_id) FROM animal) +
            (CASE WHEN n <= 9 THEN 0 WHEN n <= 17 THEN 1 WHEN n <= 24 THEN 2 WHEN n <= 30 THEN 3
                  WHEN n <= 35 THEN 4 WHEN n <= 39 THEN 5 WHEN n <= 42 THEN 6 WHEN n <= 44 THEN 7 ELSE 8 END)
        ELSE (SELECT MIN(animal_id) FROM animal) + 10 + (n MOD 80)
    END,
    -- 신청자: bulk 회원 80명 순환 (활성 USER만 — GUEST/탈퇴 미참조)
    (SELECT MIN(member_id) FROM member WHERE email LIKE 'bulk%') + (n MOD 80),
    -- 심사자: PENDING이면 NULL, 아니면 관리자
    IF(n MOD 10 = 0, NULL, (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com')),
    UUID(),
    CAST(24 + (n MOD 20) AS CHAR),
    ELT((n MOD 5) + 1, '회사원', '자영업', '학생', '프리랜서', '주부'),
    CONCAT('더미 임시보호 신청 ', n, '번입니다. 책임감 있게 돌보겠습니다.'),
    IF(n MOD 10 = 0, NULL, IF(n MOD 10 = 1, '아쉽지만 이번에는 반려합니다.', '승인합니다.')),
    ELT(CASE WHEN n MOD 10 = 0 THEN 1 WHEN n MOD 10 = 1 THEN 2
             WHEN n MOD 10 <= 4 THEN 3 WHEN n MOD 10 <= 7 THEN 4 ELSE 5 END,
        'PENDING', 'REJECTED', 'ENDED', 'FOSTERING', 'EXTENDED'),
    -- foster_start_at: 승인 계열만 (PENDING/REJECTED는 NULL)
    CASE WHEN n MOD 10 <= 1 THEN NULL
         WHEN n MOD 10 <= 4 THEN DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (100 + (n MOD 60)) DAY)   -- ENDED: 과거 시작
         ELSE DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (10 + (n MOD 30)) DAY) END,                   -- 진행중: 최근 시작
    -- foster_end_at: ENDED는 시작+20~75일(지속기간 표본), 진행중은 D+40 이후(임박 회피)
    CASE WHEN n MOD 10 <= 1 THEN NULL
         WHEN n MOD 10 <= 4 THEN DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (100 + (n MOD 60) - 20 - (n MOD 56)) DAY)
         WHEN n MOD 10 <= 7 THEN DATE_ADD(CURDATE(), INTERVAL (40 + (n MOD 57)) DAY)
         ELSE DATE_ADD(CURDATE(), INTERVAL (45 + (n MOD 36)) DAY) END,
    -- foster_extend_at: EXTENDED만
    IF(n MOD 10 >= 8, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 15) DAY), NULL),
    -- foster_complete_at: ENDED만
    IF(n MOD 10 BETWEEN 2 AND 4,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (100 + (n MOD 60) - 20 - (n MOD 56)) DAY), NULL),
    -- created_at: 최근 12개월 분산 (start보다 이틀 앞) → 월별 추이 곡선
    DATE_SUB(DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 12) MONTH), INTERVAL (n MOD 25) DAY),
    DATE_SUB(DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 12) MONTH), INTERVAL (n MOD 25) DAY)
FROM seq;

-- ── 4. is_fostered 캐시 동기화 — 진행 중(FOSTERING/EXTENDED) 활성 신청이 있는 동물만 TRUE ──
UPDATE `animal` a
SET a.is_fostered = EXISTS (
    SELECT 1 FROM foster f
    WHERE f.animal_id = a.animal_id
      AND f.deleted_at IS NULL
      AND f.status IN ('FOSTERING', 'EXTENDED'));
