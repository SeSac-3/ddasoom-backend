-- =============================================================
-- V104__seed_support_dummy.sql  [개발용 더미 — 전면 리빌드 2026-07]
-- 공지사항 / FAQ / QNA / 신고(회원 타겟) — 로컬/테스트 전용. V100 이후 실행 전제.
--
-- 검증 목표:
--  * 답변대기 QnA 카드: PENDING 12건 (명시 4 + 대량 8) — 대시보드 숫자 대조
--  * QnA는 V10 개정 반영 — answered_id/answer 컬럼 없음, 답변 = qna_comment(관리자) + answered_at
--  * 신고(MEMBER 타겟): V100의 HIDDEN 계정 3명을 대상 — "신고 확인 → 회원 숨김" 스토리 정합
--    (POST/POST_COMMENT 타겟 신고는 게시글이 생기는 V105에서 삽입 — 폴리모픽 참조 순서 보장)
--  * report 유니크 제약(uk_report_reporter_target): 신고자×대상 조합 중복 없음
--  * ⚠️ faq 테이블에는 member_id(작성자) 컬럼이 없음 — notice와 다름 (스키마 확인 완료)
-- =============================================================

-- ── 1. 공지사항 (작성자 = 관리자 adminkoo) ────────────────────────────────
INSERT INTO `notice` (`member_id`, `title`, `content`, `is_visible`, `pin_order`, `created_at`, `updated_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     '[필독] 따숨 서비스 정식 오픈 안내',
     '안녕하세요, 따숨입니다.\n\n유기동물 임시보호 및 커뮤니티 플랫폼 따숨이 정식 오픈했습니다. 서비스 이용 중 불편사항은 1:1 문의를 이용해 주세요.\n\n감사합니다.',
     TRUE, 1, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 60 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 60 DAY)),
    ((SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     '[안내] 커뮤니티 이용 수칙 및 신고 제도 안내',
     '건강한 커뮤니티를 위해 이용 수칙을 안내드립니다.\n\n1. 광고/도배 게시물은 제재 대상입니다.\n2. 욕설·비방 게시물은 신고 기능을 이용해 주세요.\n3. 신고 접수 시 관리자가 확인 후 조치합니다.',
     TRUE, 2, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY)),
    ((SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     '7월 시스템 점검 안내',
     '7월 마지막 주 새벽 2시~4시 서버 점검이 예정되어 있습니다. 점검 중 서비스 이용이 일시 중단됩니다.',
     TRUE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY)),
    ((SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     '임시보호 신청 절차 개편 안내',
     '임시보호 신청 심사 기준이 개편되었습니다. 자세한 내용은 임시보호 페이지를 확인해 주세요.',
     TRUE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 7 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 7 DAY)),
    ((SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     '입양후기 게시판 오픈!',
     '임보/입양 경험을 나눌 수 있는 입양후기 게시판이 열렸습니다. 따뜻한 이야기를 들려주세요.',
     TRUE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 5 DAY)),
    ((SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
     '(비공개 테스트) 내부 공지 초안',
     '이 공지는 is_visible=FALSE로 사용자 목록에 노출되지 않아야 합니다.',
     FALSE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY));

-- ── 2. FAQ (작성자 컬럼 없음 — category/question/answer/is_visible 구조) ──
-- ⚠️ faq.category는 FaqCategory enum (@Enumerated STRING) — DB엔 "상수명"(영문)을 저장.
--    한글 label(회원/계정, 임시보호 등)을 넣으면 조회 시 enum 변환 실패(No enum constant). 반드시 상수명 사용.
--    상수: ACCOUNT / FOSTER / ANIMAL_INFO / COMMUNITY / SERVICE / ETC
INSERT INTO `faq` (`category`, `question`, `answer`, `is_visible`, `created_at`, `updated_at`) VALUES
    ('ACCOUNT',
     '회원 탈퇴 후 같은 이메일로 재가입할 수 있나요?',
     '탈퇴한 이메일로는 재가입이 불가능합니다. 계정 복구가 필요하시면 고객센터로 문의해 주세요.',
     TRUE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 50 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 50 DAY)),
    ('ACCOUNT',
     '소셜 계정으로 가입했는데 비밀번호 변경이 안 돼요.',
     '소셜 로그인 전용 회원은 비밀번호가 없어 비밀번호 변경 기능을 이용할 수 없습니다.',
     TRUE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 48 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 48 DAY)),
    ('FOSTER',
     '임시보호 기간은 얼마나 되나요?',
     '기본 30일이며, 상황에 따라 연장 신청이 가능합니다. 연장은 관리자 승인 후 반영됩니다.',
     TRUE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 45 DAY)),
    ('FOSTER',
     '임시보호 신청 후 결과는 언제 알 수 있나요?',
     '관리자 심사 후 보통 2~3일 내 답변드립니다. 마이페이지에서 신청 상태를 확인할 수 있습니다.',
     TRUE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 43 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 43 DAY)),
    ('COMMUNITY',
     '게시글을 신고하면 어떻게 처리되나요?',
     '신고가 접수되면 관리자가 내용을 직접 확인한 뒤 게시글 숨김 또는 작성자 제재 등의 조치를 합니다.',
     TRUE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 40 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 40 DAY)),
    ('ETC',
     '(비공개 테스트) 노출 제외 FAQ',
     'is_visible=FALSE — 사용자 목록에 나오면 안 됩니다.',
     FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 38 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 38 DAY));

-- ── 3. QnA — 명시 4건 (PENDING 4: 답변대기 시연 대상) ────────────────────
INSERT INTO `qna` (`questioner_id`, `title`, `content`, `status`, `is_visible`, `answered_at`, `created_at`, `updated_at`) VALUES
    ((SELECT member_id FROM member WHERE email = 'user01@ddasoom.com'),
     '임보 신청서를 수정하고 싶어요', '어제 제출한 임시보호 신청서에 오타가 있어서 수정하고 싶습니다. 방법이 있을까요?',
     'PENDING', TRUE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user02@ddasoom.com'),
     '프로필 사진이 업로드되지 않아요', '10MB 이하 jpg인데도 업로드가 실패합니다. 확인 부탁드립니다.',
     'PENDING', TRUE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY)),
    ((SELECT member_id FROM member WHERE email = 'user03@ddasoom.com'),
     '탈퇴한 계정 복구 문의', '실수로 탈퇴했는데 복구 가능한가요? 임보 이력이 남아있으면 좋겠습니다.',
     'PENDING', FALSE, NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 12 HOUR), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 12 HOUR)),
    ((SELECT member_id FROM member WHERE email = 'today01@ddasoom.com'),
     '가입 인사 겸 질문드려요', '오늘 가입했습니다! 임보 신청에 필요한 서류가 따로 있나요?',
     'PENDING', TRUE, NULL, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));

-- ── 4. QnA — 대량 36건 (PENDING 8 / ANSWERED 28) ────────────────────────
-- n MOD 9 IN (0, 4) 이면 PENDING → 36건 중 8건
INSERT INTO `qna` (`questioner_id`, `title`, `content`, `status`, `is_visible`, `answered_at`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 36
)
SELECT
    (SELECT MIN(member_id) FROM member WHERE email LIKE 'bulk%') + (n MOD 80),
    CONCAT('더미 문의 ', n, ' — ', ELT((n MOD 4) + 1, '계정 관련', '임시보호 관련', '커뮤니티 관련', '기타')),
    CONCAT('더미 문의 본문 ', n, '입니다. 확인 부탁드립니다.'),
    IF(n MOD 9 IN (0, 4), 'PENDING', 'ANSWERED'),
    IF(n MOD 7 = 0, FALSE, TRUE),
    IF(n MOD 9 IN (0, 4), NULL, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 40) DAY)),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL ((n MOD 40) + 2) DAY),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (n MOD 40) DAY)
FROM seq;

-- ANSWERED 건에 관리자 답변 댓글 1개씩 (대화형 구조 — V10)
INSERT INTO `qna_comment` (`qna_id`, `member_id`, `content`, `created_at`, `updated_at`)
SELECT q.qna_id,
       (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com'),
       CONCAT('안녕하세요, 따숨입니다. 문의하신 내용 확인했습니다. 답변드립니다 — 더미 답변 ', q.qna_id, '번.'),
       q.answered_at, q.answered_at
FROM qna q
WHERE q.status = 'ANSWERED';

-- ── 5. 신고 — MEMBER 타겟 9건 (HIDDEN 3명 × 신고자 3명) ──────────────────
-- hidden01/02/03이 다수 신고를 받은 상황. 일부는 APPROVED(→ 이미 HIDDEN 처리됨 스토리),
-- 일부는 PENDING(관리자 신고 큐에 남아있는 시연 대상), 1건 REJECTED(허위 판정).
INSERT INTO `report` (`reporter_id`, `target_type`, `target_id`, `reason`, `content`, `status`, `processed_id`, `processed_at`, `created_at`, `updated_at`)
SELECT r.member_id, 'MEMBER', t.member_id,
       ELT((r.rn + t.rn) MOD 3 + 1, 'SPAM', 'ABUSE', 'INAPPROPRIATE'),
       CONCAT(t.email, ' 회원이 반복적으로 문제 행동을 합니다.'),
       CASE WHEN t.rn = 1 THEN 'APPROVED'                       -- hidden01: 전부 승인 처리 완료
            WHEN t.rn = 2 AND r.rn = 1 THEN 'REJECTED'          -- hidden02: 1건 허위 판정
            WHEN t.rn = 2 THEN 'APPROVED'
            ELSE 'PENDING' END,                                 -- hidden03: 미처리 큐 (시연 대상)
       CASE WHEN t.rn = 3 THEN NULL ELSE (SELECT member_id FROM member WHERE email = 'adminkoo@ddasoom.com') END,
       CASE WHEN t.rn = 3 THEN NULL ELSE DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL t.rn DAY) END,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (t.rn + r.rn + 2) DAY),
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL t.rn DAY)
FROM (SELECT member_id, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
      FROM member WHERE email IN ('user01@ddasoom.com', 'user02@ddasoom.com', 'user03@ddasoom.com')) r
CROSS JOIN (SELECT member_id, email, ROW_NUMBER() OVER (ORDER BY member_id) AS rn
            FROM member WHERE email IN ('hidden01@ddasoom.com', 'hidden02@ddasoom.com', 'hidden03@ddasoom.com')) t;