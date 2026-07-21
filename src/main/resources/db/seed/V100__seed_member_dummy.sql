-- =============================================================
-- V100__seed_member_dummy.sql  [개발용 더미 — 전면 리빌드 2026-07]
-- 로컬/테스트 전용 (db/seed) — 운영 DB 미포함. fresh 마이그레이션 전제(기존 로컬 DB는 밀고 재기동).
--
-- 통계/대시보드/관리자페이지 검증 목표:
--  * 오늘 신규 가입자 카드: 오늘 가입 3명 고정 (명시 행)
--  * 일별 가입 추이(최근 7일~과거): 대량 행의 가입일을 CURDATE() 상대값으로 최근 60일 분산
--  * 회원 상태 3분류: HIDDEN 3명(신고 제재 시연), 탈퇴 4명(복구 시연), 나머지 ACTIVE
--  * GUEST 3명(소셜 미완료 — nickname NULL, USER_URIS 403 검증용)
--  * 이벤트: user01/user02 PARTICIPATING 유지 (V105 게시글 10개+ 연동)
--  * 로그인 이력: 명시 유저에 다건 (관리자 상세/마이페이지 페이징 검증)
-- 비밀번호는 전원 동일 해시(평문 Ddasoom1!) — 기존 시드와 동일
-- =============================================================

SET @pw := '$2b$10$nFgxKiqsJPXAW3uWp5/tqemYRrE1LUkzhftrV4GvrYVJ3r2XF9VN2';

-- ── 1. 명시 회원 (시연 식별용 — 이름으로 찾을 수 있는 계정들) ──────────────

-- 오늘 가입 3명 (대시보드 "오늘 신규 가입자" = 이 3명 + 없음 → 항상 3으로 검증 가능)
INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `status`, `created_at`, `updated_at`) VALUES
    ('today01@ddasoom.com', @pw, '오늘가입일', '오늘가입한집사1', '01033330001', 'USER', 'NONE', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('today02@ddasoom.com', @pw, '오늘가입이', '오늘가입한집사2', '01033330002', 'USER', 'NONE', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('today03@ddasoom.com', @pw, '오늘가입삼', '오늘가입한집사3', '01033330003', 'USER', 'NONE', 'ACTIVE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- 기본 활성 USER (기존 시드 인물 유지 — 팀원들이 아는 계정. user01/02는 이벤트 참여중)
INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `status`, `created_at`, `updated_at`) VALUES
    ('user01@ddasoom.com', @pw, '박하늘', '하늘이집사', '01022220001', 'USER', 'PARTICIPATING', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 45 DAY)),
    ('user02@ddasoom.com', @pw, '최바다', '바다냥집사', '01022220002', 'USER', 'PARTICIPATING', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 40 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 40 DAY)),
    ('user03@ddasoom.com', @pw, '정온유', '온유멍집사', '01022220003', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 35 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 35 DAY)),
    ('user04@ddasoom.com', @pw, '한별님', '별님보호소', '01022220004', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY)),
    ('user05@ddasoom.com', @pw, '오솜이', '솜이누나', '01022220005', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY)),
    ('user06@ddasoom.com', @pw, '김도담', '도담도담', '01022220006', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY)),
    ('user07@ddasoom.com', @pw, '이봄날', '봄날의멍멍', '01022220007', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 15 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 15 DAY)),
    ('user08@ddasoom.com', @pw, '서루비', '루비언니', '01022220008', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY)),
    ('user09@ddasoom.com', @pw, '문찬비', '찬비네냥이', '01022220009', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY)),
    ('user10@ddasoom.com', @pw, '강마루', '마루아빠', '01022220010', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY));

-- 신고 제재(HIDDEN) 3명 — 관리자 회원 목록 상태 3분류·상세 "숨김 해제" 버튼 시연용
-- (V104의 MEMBER 타겟 신고가 이 계정들을 신고 대상으로 참조 → "신고 확인 → 회원 숨김" 스토리 완결)
INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `status`, `created_at`, `updated_at`) VALUES
    ('hidden01@ddasoom.com', @pw, '차막말', '도배꾼', '01044440001', 'USER', 'NONE', 'HIDDEN', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 50 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    ('hidden02@ddasoom.com', @pw, '남광고', '광고쟁이', '01044440002', 'USER', 'NONE', 'HIDDEN', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 48 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY)),
    ('hidden03@ddasoom.com', @pw, '구시비', '시비왕', '01044440003', 'USER', 'NONE', 'HIDDEN', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 46 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY));

-- 탈퇴 회원 4명 — 상세 "계정 복구" 버튼, 탈퇴 회원 재가입 불가(AUTH_001), getMember 활성 검사 시연용
INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `status`, `created_at`, `updated_at`, `deleted_at`) VALUES
    ('left01@ddasoom.com', @pw, '지떠남', '떠난집사1', '01055550001', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 55 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 7 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 7 DAY)),
    ('left02@ddasoom.com', @pw, '고졸업', '떠난집사2', '01055550002', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 54 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 6 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 6 DAY)),
    ('left03@ddasoom.com', @pw, '노관심', '떠난집사3', '01055550003', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 53 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY)),
    ('left04@ddasoom.com', @pw, '하작별', '떠난집사4', '01055550004', 'USER', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 52 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY));

-- GUEST 3명 — 소셜 가입 후 추가정보 미입력 (password/nickname NULL). USER_URIS 403·승급 흐름 검증용
INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `status`, `created_at`, `updated_at`) VALUES
    ('guest01@ddasoom.com', NULL, NULL, NULL, NULL, 'GUEST', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 8 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 8 DAY)),
    ('guest02@ddasoom.com', NULL, NULL, NULL, NULL, 'GUEST', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY)),
    ('guest03@ddasoom.com', NULL, NULL, NULL, NULL, 'GUEST', 'NONE', 'ACTIVE', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY));

-- GUEST의 소셜 연동 레코드 (provider+provider_id가 소셜 신원 — 이메일 아님)
INSERT INTO `member_social` (`member_id`, `provider`, `provider_id`, `created_at`, `updated_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'guest01@ddasoom.com'), 'KAKAO',  'kakao-dummy-0001', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ((SELECT member_id FROM member WHERE email = 'guest02@ddasoom.com'), 'NAVER',  'naver-dummy-0001', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ((SELECT member_id FROM member WHERE email = 'guest03@ddasoom.com'), 'GOOGLE', 'google-dummy-0001', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- ── 2. 대량 회원 80명 (bulk01~bulk80) — 가입 추이 곡선용 ─────────────────
-- 가입일: 최근 60일에 분산 (n MOD 60일 전) → 일별 추이 어느 구간을 보든 데이터 존재.
-- 시간대(n MOD 14시)도 흩뿌려 "자정 몰림" 없는 자연스러운 곡선.
INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `status`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 80
)
SELECT
    CONCAT('bulk', LPAD(n, 2, '0'), '@ddasoom.com'),
    @pw,
    CONCAT('더미회원', n),
    CONCAT('더미닉네임', n),
    CONCAT('0109999', LPAD(n, 4, '0')),
    'USER', 'NONE', 'ACTIVE',
    DATE_SUB(DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 60) DAY), INTERVAL (n MOD 14) HOUR),
    DATE_SUB(DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 60) DAY), INTERVAL (n MOD 14) HOUR)
FROM seq;

-- ── 3. 로그인 이력 ──────────────────────────────────────────────────────
-- user01: 23건 (관리자 상세 로그인 이력 페이징 20건/페이지 → 2페이지 검증용)
INSERT INTO `login_log` (`member_id`, `login_type`, `created_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 23
)
SELECT
    (SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'),
    ELT((n MOD 4) + 1, 'LOCAL', 'KAKAO', 'NAVER', 'GOOGLE'),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL n DAY)
FROM seq;

-- 명시 유저들 최근 로그인 몇 건씩 (마이페이지 최근 5건 미리보기용)
INSERT INTO `login_log` (`member_id`, `login_type`, `created_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'user02@ddasoom.com'), 'LOCAL', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user02@ddasoom.com'), 'KAKAO', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'), 'LOCAL', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY)),
    ((SELECT member_id FROM member WHERE email = 'hidden01@ddasoom.com'), 'LOCAL', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY)),
    ((SELECT member_id FROM member WHERE email = 'today01@ddasoom.com'), 'LOCAL', CURRENT_TIMESTAMP(6));
