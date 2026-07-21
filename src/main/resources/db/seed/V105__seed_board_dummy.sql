-- =============================================================
-- V105__seed_board_dummy.sql  [개발용 더미 — 신설 2026-07 / board_type·category 정정]
-- 게시판(post/post_comment) + 게시글·댓글 타겟 신고 — 로컬/테스트 전용. V100·V104 이후 실행 전제.
--
-- ⚠️ 정정 (2026-07): board_type/category를 실제 BoardType enum 및 프론트 규칙에 맞춤.
--   * BoardType enum = ADOPTION_REVIEW / DOG_INFO / CAT_INFO  (기존 PET_INFO는 오기 — 스키마 주석이 구버전이었음)
--   * 종(강아지/고양이)은 category가 아니라 board_type으로 분리 (DOG_INFO/CAT_INFO)
--   * category는 "보드 내 세부 주제" 자유문자열:
--       - DOG_INFO / CAT_INFO → category '예방접종' (프론트 categories: ['예방접종'])
--       - ADOPTION_REVIEW      → category '강아지'/'고양이' (펫 종류)
--
-- 검증 목표:
--   * 게시판 3종: ADOPTION_REVIEW(입양후기) / DOG_INFO(강아지) / CAT_INFO(고양이) 목록·필터
--   * 이벤트 연동: user01·user02가 이벤트 기간(7월) 게시글 10건+ → EventStatus PARTICIPATING 정합
--   * 신고 시연: HIDDEN 회원이 쓴 도배/시비 게시글 → POST/POST_COMMENT 타겟 신고
--   * comment_count 캐시 정합: 댓글 삽입 후 일괄 동기화 UPDATE
-- =============================================================

-- ── 1. 명시 게시글 — 입양후기 6건 (category = 펫 종류) ─────────────────────
INSERT INTO `post` (`member_id`, `board_type`, `category`, `title`, `content`, `view_count`, `comment_count`, `created_at`, `updated_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'), 'ADOPTION_REVIEW', '강아지',
     '초코와 함께한 한 달, 그리고 입양 결정까지',
     '임시보호로 시작했는데 이제는 가족이 되었습니다. 처음엔 경계가 심했던 초코가 지금은 제 무릎에서 잠들어요. 임보를 고민하시는 분들께 꼭 말씀드리고 싶어요 — 용기 내시길.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 20 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user04@ddasoom.com'), 'ADOPTION_REVIEW', '강아지',
     '진돗개 보리 임보 후기 (대형견 임보 팁 포함)',
     '대형견 임보가 처음이라 걱정 많았는데, 산책 루틴만 잡히니 순한 양이 됩니다. 대형견 임보 팁: 첫 주는 산책 짧게 여러 번, 급여량은 보호소 기준 유지.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 15 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 15 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user05@ddasoom.com'), 'ADOPTION_REVIEW', '고양이',
     '까망이 입양 후기 — 검은 고양이 편견을 버리세요',
     '검은 고양이는 입양이 잘 안 된다고 하죠. 까망이는 제가 만난 가장 다정한 생명입니다. 매일 아침 이마 박치기로 저를 깨워요.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 12 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 12 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user06@ddasoom.com'), 'ADOPTION_REVIEW', '강아지',
     '리트리버 탄이, 임보 3주차 근황',
     '24kg 대형견과의 동거는 체력전이지만 그만큼 행복도 큽니다. 탄이는 아이들과도 잘 지내요. 이번 주말 입양 상담 예정입니다.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 8 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 8 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user07@ddasoom.com'), 'ADOPTION_REVIEW', '고양이',
     '치즈태비 치즈 임보 일기 — 겁쟁이가 개냥이 되기까지',
     '첫 일주일은 침대 밑에서 나오지 않았어요. 3주차인 지금은 제 어깨에 올라옵니다. 시간이 약이라는 말, 임보에서 배웁니다.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user08@ddasoom.com'), 'ADOPTION_REVIEW', '강아지',
     '임보 준비물 체크리스트 공유합니다',
     '임보 3회차가 되니 준비물이 정리되네요. 켄넬, 배변패드, 이동장, 그리고 가장 중요한 것 — 인내심. 체크리스트 본문에 정리했습니다.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY));

-- ── 2. 명시 게시글 — 강아지(DOG_INFO) 3건 + 고양이(CAT_INFO) 2건 + 신고대상 도배 3건 ──
-- category는 '예방접종'(정보 게시판 세부 주제). 신고 대상은 HIDDEN 회원 작성.
INSERT INTO `post` (`member_id`, `board_type`, `category`, `title`, `content`, `view_count`, `comment_count`, `created_at`, `updated_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'), 'DOG_INFO', '예방접종',
     '여름철 강아지 산책 시간대 어떻게 하시나요?',
     '한낮 아스팔트가 너무 뜨거워서 새벽/저녁으로 옮겼는데 다들 몇 시쯤 나가시나요?',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 6 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 6 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user10@ddasoom.com'), 'DOG_INFO', '예방접종',
     '동물병원 야간 진료 리스트 (지역별 정리 중)',
     '응급상황 대비 야간 진료 가능한 병원을 지역별로 모으고 있습니다. 댓글로 제보 부탁드려요!',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY)),
    ((SELECT member_id FROM member WHERE email = 'hidden03@ddasoom.com'), 'DOG_INFO', '예방접종',
     '(욕설 포함 시비성 게시글)',
     '다른 회원을 비방하는 내용의 게시글입니다. 신고 처리 시연용.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user02@ddasoom.com'), 'CAT_INFO', '예방접종',
     '고양이 습식 사료 추천 부탁드려요',
     '입 짧은 냥이라 습식 브랜드 몇 개 돌려봤는데 잘 안 먹네요. 기호성 좋은 제품 추천 부탁드립니다.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user09@ddasoom.com'), 'CAT_INFO', '예방접종',
     '장모종 여름 미용, 하시나요 마시나요?',
     '터키시앙고라 임보 중인데 여름 미용 의견이 갈리더라고요. 경험담 들려주세요.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY)),
    -- 신고 대상 도배 게시글 (HIDDEN 회원 작성 — "왜 숨김됐는가" 증거물)
    ((SELECT member_id FROM member WHERE email = 'hidden01@ddasoom.com'), 'DOG_INFO', '예방접종',
     '■■최저가 사료 공동구매 링크■■ 클릭!!',
     '지금 바로 클릭하세요!! 최저가 보장!! (외부 링크 도배)',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY)),
    ((SELECT member_id FROM member WHERE email = 'hidden02@ddasoom.com'), 'CAT_INFO', '예방접종',
     '분양 홍보합니다 (품종묘 분양)',
     '커뮤니티 취지와 맞지 않는 상업적 분양 홍보 글입니다.',
     0, 0, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY));

-- ── 3. 이벤트 참여 게시글 — user01/user02 각 11건 (이벤트 기간 7월, 10건+ 판정 충족) ──
-- board_type: 강아지/고양이 번갈아, category '예방접종'. created_at = 7월 초~중순 고정.
INSERT INTO `post` (`member_id`, `board_type`, `category`, `title`, `content`, `view_count`, `comment_count`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 22
)
SELECT
    IF(n <= 11,
       (SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'),
       (SELECT member_id FROM member WHERE email = 'user02@ddasoom.com')),
    IF(n MOD 2 = 0, 'DOG_INFO', 'CAT_INFO'),
    '예방접종',
    CONCAT('이벤트 참여 더미 글 ', n, ' — 오늘의 반려 일상'),
    CONCAT('이벤트 기간 게시글 카운트용 더미 본문 ', n, '입니다.'),
    0, 0,
    DATE_ADD(MAKEDATE(YEAR(CURDATE()), 1), INTERVAL (181 + (n MOD 14)) DAY),   -- 7월 1일 + 0~13일
    DATE_ADD(MAKEDATE(YEAR(CURDATE()), 1), INTERVAL (181 + (n MOD 14)) DAY)
FROM seq;

-- ── 4. 대량 게시글 66건 — 3종 게시판 순환 (목록 페이징·필터·활동량 곡선용) ──
-- n MOD 3: 0=입양후기(category 강아지/고양이), 1=강아지정보, 2=고양이정보(category 예방접종)
INSERT INTO `post` (`member_id`, `board_type`, `category`, `title`, `content`, `view_count`, `comment_count`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 66
)
SELECT
    (SELECT MIN(member_id) FROM member WHERE email LIKE 'bulk%') + (n MOD 80),
    ELT((n MOD 3) + 1, 'ADOPTION_REVIEW', 'DOG_INFO', 'CAT_INFO'),
    CASE WHEN n MOD 3 = 0 THEN IF(n MOD 2 = 0, '강아지', '고양이')   -- 입양후기: 펫 종류
                          ELSE '예방접종' END,                        -- 정보 게시판: 세부 주제
    CONCAT(ELT((n MOD 3) + 1, '[후기] 더미 입양·임보 후기 ', '[강아지] 더미 정보글 ', '[고양이] 더미 정보글 '), n),
    CONCAT('더미 게시글 본문 ', n, '입니다. 목록 페이징과 검색 필터 검증용 데이터입니다.'),
    (n * 13) MOD 300,                                            -- 조회수 0~299 (인기순 검증)
    0,
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 45) DAY),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 45) DAY)
FROM seq;

-- ── 5. 댓글 — 명시 글에 수작업 6건 + 대댓글 1건 + 대량 60건 ────────────────
INSERT INTO `post_comment` (`post_id`, `member_id`, `parent_comment_id`, `content`, `created_at`, `updated_at`) VALUES
    ((SELECT post_id FROM post WHERE title = '초코와 함께한 한 달, 그리고 입양 결정까지'),
     (SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'), NULL,
     '읽다가 눈물이… 초코 행복하게 해주셔서 감사해요.', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 19 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 19 DAY)),
    ((SELECT post_id FROM post WHERE title = '초코와 함께한 한 달, 그리고 입양 결정까지'),
     (SELECT member_id FROM member WHERE email = 'user02@ddasoom.com'), NULL,
     '저도 임보 고민 중이었는데 용기 얻고 갑니다!', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 18 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 18 DAY)),
    ((SELECT post_id FROM post WHERE title = '까망이 입양 후기 — 검은 고양이 편견을 버리세요'),
     (SELECT member_id FROM member WHERE email = 'user09@ddasoom.com'), NULL,
     '검은 고양이 최고죠. 까망이 오래오래 행복하길!', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 11 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 11 DAY)),
    ((SELECT post_id FROM post WHERE title = '여름철 강아지 산책 시간대 어떻게 하시나요?'),
     (SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'), NULL,
     '저는 아침 6시요. 손등으로 아스팔트 5초 체크하고 나갑니다.', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY)),
    ((SELECT post_id FROM post WHERE title = '고양이 습식 사료 추천 부탁드려요'),
     (SELECT member_id FROM member WHERE email = 'user07@ddasoom.com'), NULL,
     '기호성은 토핑용 츄르 살짝 섞는 게 저희 집 해법이었어요.', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    -- 신고 대상 댓글 (HIDDEN 회원의 시비성 댓글 — POST_COMMENT 타겟 신고 대상)
    ((SELECT post_id FROM post WHERE title = '여름철 강아지 산책 시간대 어떻게 하시나요?'),
     (SELECT member_id FROM member WHERE email = 'hidden03@ddasoom.com'), NULL,
     '(비방성 댓글) 그것도 모르면서 개를 왜 키움?', DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY));

-- 대댓글 1건 (계층 구조 검증)
-- ⚠️ MySQL은 INSERT 대상 테이블(post_comment)을 같은 문장 서브쿼리에서 직접 참조 금지(에러 1093).
--    부모 댓글 조회를 파생 테이블(SELECT * FROM (...) AS x)로 한 겹 감싸 우회한다.
INSERT INTO `post_comment` (`post_id`, `member_id`, `parent_comment_id`, `content`, `created_at`, `updated_at`)
SELECT
    (SELECT post_id FROM post WHERE title = '초코와 함께한 한 달, 그리고 입양 결정까지'),
    (SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'),
    parent.id,
    '감사합니다! 궁금한 점 있으면 편하게 물어보세요.',
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 17 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 17 DAY)
FROM (SELECT post_comment_id AS id FROM post_comment
      WHERE content = '저도 임보 고민 중이었는데 용기 얻고 갑니다!' LIMIT 1) AS parent;

-- 대량 댓글 60건 — 대량 게시글에 순환 배분
INSERT INTO `post_comment` (`post_id`, `member_id`, `parent_comment_id`, `content`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 60
)
SELECT
    p.post_id,
    (SELECT MIN(member_id) FROM member WHERE email LIKE 'bulk%') + ((n * 3) MOD 80),
    NULL,
    CONCAT('더미 댓글 ', n, '입니다. 좋은 정보 감사합니다!'),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 40) DAY),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 40) DAY)
FROM seq
JOIN (SELECT post_id, ROW_NUMBER() OVER (ORDER BY post_id) AS rn
      FROM post WHERE title LIKE '%더미%') p
  ON p.rn = (seq.n MOD 66) + 1;

-- ── 6. 신고 — POST 타겟 3건 + POST_COMMENT 타겟 3건 (관리자 신고 큐 시연) ──
INSERT INTO `report` (`reporter_id`, `target_type`, `target_id`, `reason`, `content`, `status`, `processed_id`, `processed_at`, `created_at`, `updated_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'), 'POST',
     (SELECT post_id FROM post WHERE title = '■■최저가 사료 공동구매 링크■■ 클릭!!'),
     'SPAM', '외부 링크 도배 광고 게시글입니다.', 'PENDING', NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 4 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user04@ddasoom.com'), 'POST',
     (SELECT post_id FROM post WHERE title = '분양 홍보합니다 (품종묘 분양)'),
     'INAPPROPRIATE', '상업적 분양 홍보 — 커뮤니티 취지 위반입니다.', 'PENDING', NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user05@ddasoom.com'), 'POST',
     (SELECT post_id FROM post WHERE title = '(욕설 포함 시비성 게시글)'),
     'ABUSE', '욕설과 비방이 포함된 게시글입니다.', 'APPROVED',
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY),
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'), 'POST_COMMENT',
     (SELECT post_comment_id FROM (SELECT post_comment_id FROM post_comment
        WHERE content = '(비방성 댓글) 그것도 모르면서 개를 왜 키움?' LIMIT 1) AS c1),
     'ABUSE', '다른 회원을 비방하는 댓글입니다.', 'PENDING', NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user07@ddasoom.com'), 'POST_COMMENT',
     (SELECT post_comment_id FROM (SELECT post_comment_id FROM post_comment
        WHERE content = '(비방성 댓글) 그것도 모르면서 개를 왜 키움?' LIMIT 1) AS c2),
     'ABUSE', '동일 댓글 중복 신고 (다른 신고자 — 유니크 제약 검증).', 'PENDING', NULL, NULL,
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user08@ddasoom.com'), 'POST',
     (SELECT post_id FROM post WHERE title = '■■최저가 사료 공동구매 링크■■ 클릭!!'),
     'ETC', '같은 글이 다른 게시판에도 반복 게시되고 있습니다.', 'REJECTED',
     (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY),
     DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY));

-- ── 7. comment_count 캐시 동기화 (활성 댓글 수와 일치 보장) ─────────────────
UPDATE `post` p
SET p.comment_count = (SELECT COUNT(*) FROM post_comment c
                       WHERE c.post_id = p.post_id AND c.deleted_at IS NULL);