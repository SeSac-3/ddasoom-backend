-- =============================================================
-- V103__seed_report.sql  [개발용 더미 — 신설 2026-07 / V14 스냅샷 대응]
-- 로컬/테스트 전용 (db/seed) — 운영 DB 미포함. V100·V101·V102 이후 실행 전제.
--
-- 배경: V14가 report에 스냅샷 4컬럼(reported_member_id / target_parent_id /
--       target_title / target_snippet)을 추가했으나, 기존 시드(V101·V102)로 들어간
--       신고 15건은 전부 NULL이다. 그대로 두면 관리자 신고 화면이 전건 "정보 없음"으로 뜬다.
--
-- 검증 목표:
--   1. 스냅샷 백필      — 기존 신고 15건에 피신고자·제목·발췌·원글ID 채우기
--   2. 승인 이력 정합성 — APPROVED 신고인데 대상이 멀쩡히 살아있는 모순 제거
--                         (새 정책: 승인 = 콘텐츠 삭제 + 피신고자 강제탈퇴)
--   3. 시연 시나리오 2종 — 이미 탈퇴한 회원 / ADMIN 대상(no-op)
--
-- ⚠️ 실제 앱은 본문 HTML을 Jsoup으로 평문화한 뒤 200자를 저장한다.
--    시드 본문은 태그 없는 평문이라 여기서는 LEFT()로 충분하다.
-- ⚠️ 백필 UPDATE에서 updated_at을 자기 자신으로 세팅하는 이유:
--    report.updated_at은 ON UPDATE CURRENT_TIMESTAMP(6)라 명시하지 않으면 전건이 오늘로 갱신된다.
--    SET에 컬럼을 명시하면 MySQL이 자동 갱신을 건너뛴다 (시드의 시간 분포 보존).
-- =============================================================


-- ── 1. 기존 신고 스냅샷 백필 ────────────────────────────────────────────────

-- 1-1) POST 대상 — 소유자 = 게시글 작성자, 제목/발췌 = 게시글 본인 것
UPDATE `report` r
    JOIN `post` p ON p.post_id = r.target_id
SET r.reported_member_id = p.member_id,
    r.target_parent_id   = NULL,
    r.target_title       = p.title,
    r.target_snippet     = LEFT(p.content, 200),
    r.updated_at         = r.updated_at
WHERE r.target_type = 'POST';

-- 1-2) POST_COMMENT 대상 — 소유자 = 댓글 작성자, 제목은 '원글' 제목.
--      댓글 본문만으로는 관리자가 어떤 맥락의 발언인지 알 수 없어 원글 제목을 함께 박아둔다.
--      target_parent_id는 프론트가 /admin/posts/{원글}#comment-{댓글} 링크를 조합하는 데 쓴다.
UPDATE `report` r
    JOIN `post_comment` c ON c.post_comment_id = r.target_id
    JOIN `post` p ON p.post_id = c.post_id
SET r.reported_member_id = c.member_id,
    r.target_parent_id   = c.post_id,
    r.target_title       = p.title,
    r.target_snippet     = LEFT(c.content, 200),
    r.updated_at         = r.updated_at
WHERE r.target_type = 'POST_COMMENT';

-- 1-3) MEMBER 대상 — 대상 본인이 곧 피신고자. 본문이 없으므로 snippet은 NULL,
--      제목 자리에 닉네임을 넣어 프론트가 targetType 분기 없이 동일하게 렌더할 수 있게 한다.
UPDATE `report` r
    JOIN `member` m ON m.member_id = r.target_id
SET r.reported_member_id = m.member_id,
    r.target_parent_id   = NULL,
    r.target_title       = m.nickname,
    r.target_snippet     = NULL,
    r.updated_at         = r.updated_at
WHERE r.target_type = 'MEMBER';


-- ── 2. 승인 이력 정합성 보정 ────────────────────────────────────────────────
-- 기존 시드는 신고만 APPROVED로 넣고 대상은 건드리지 않아, 승인됐는데 게시글이 살아있고
-- 피신고자도 활성인 모순 상태였다. 새 정책(승인 = 콘텐츠 삭제 + 강제탈퇴)에 맞춰 보정한다.
--
-- 시연 역할 분담:
--   hidden01(도배꾼)   → 탈퇴 + 게시글 삭제. '처리 완료 이력 열람' 시연
--   hidden02(광고쟁이) → 활성 유지 + PENDING 1건. '라이브 승인' 시연 (버튼 눌러 전 과정 관찰)
--   hidden03(시비왕)   → 활성 유지 + PENDING 3건. '상습범' 시연 (회원 누적이 대상 누적보다 큼)

-- 2-1) V102의 APPROVED 건(hidden03 게시글)을 PENDING으로 되돌린다.
--      그대로 두면 hidden03이 탈퇴 상태가 되어, 승인 버튼을 직접 눌러보는 시연 대상이 사라진다.
--      상습범 시연과 라이브 승인을 hidden02·hidden03에 몰아주기 위한 조정.
UPDATE `report`
SET `status`       = 'PENDING',
    `processed_id` = NULL,
    `processed_at` = NULL,
    `updated_at`   = `updated_at`
WHERE `target_type` = 'POST'
  AND `target_id` = (SELECT post_id FROM post WHERE title = '(비방성 게시글) 여기 사람들 수준이');

-- 2-2) 승인 완료로 남은 신고의 대상 게시글 삭제
--      (2-1 이후 APPROVED로 남는 건 hidden01 대상 MEMBER 신고뿐이라 POST 삭제는 0건일 수 있다.
--       V101의 MEMBER 신고 처리 결과에 따라 달라지므로 조건부로 둔다.)
UPDATE `post` p
SET p.deleted_at = DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY),
    p.updated_at = p.updated_at
WHERE p.deleted_at IS NULL
  AND p.post_id IN (
    SELECT target_id FROM (
        SELECT DISTINCT r.target_id
        FROM report r
        WHERE r.target_type = 'POST' AND r.status = 'APPROVED'
    ) AS approved_posts
);

-- 2-3) 승인 완료된 신고의 피신고자 강제탈퇴
--      status는 V100에서 이미 HIDDEN이므로 deleted_at만 세팅하면 앱의 withdrawByReport 결과와 동일해진다.
--      (Member.softDelete()는 deleted_at만 채우고 익명화는 하지 않는다 — 관리자가 이력을 추적해야 하므로)
--      ADMIN 제외: 앱의 withdrawByReport도 ADMIN은 no-op으로 넘긴다. 시드가 앱보다 과하게 처리하면 안 된다.
UPDATE `member` m
SET m.deleted_at = DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY),
    m.status     = 'HIDDEN',
    m.updated_at = m.updated_at
WHERE m.deleted_at IS NULL
  AND m.role <> 'ADMIN'
  AND m.member_id IN (
    SELECT reported_member_id FROM (
        SELECT DISTINCT r.reported_member_id
        FROM report r
        WHERE r.status = 'APPROVED' AND r.reported_member_id IS NOT NULL
    ) AS sanctioned
);

-- 2-4) 탈퇴 처리된 회원의 게시글도 함께 삭제
--      앱은 '승인된 신고의 대상 콘텐츠'만 지우지만, 시드는 탈퇴 회원의 글이 목록에 남아
--      "탈퇴했는데 글이 보인다"는 오해를 주지 않도록 함께 정리한다.
UPDATE `post` p
    JOIN `member` m ON m.member_id = p.member_id
SET p.deleted_at = DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY),
    p.updated_at = p.updated_at
WHERE p.deleted_at IS NULL
  AND m.deleted_at IS NOT NULL
  AND m.status = 'HIDDEN';


-- ── 3. 시연 시나리오 신고 추가 ──────────────────────────────────────────────
-- uk_report_reporter_target(신고자 × 대상 유형 × 대상) 충돌이 없도록 신고자를 새로 배정했다.
-- (V101은 user01~03이 MEMBER를, V102는 user01·03·04·05·07·08이 POST/COMMENT를 신고했다)

-- 3-1) 이미 탈퇴한 회원 대상 — 승인 다이얼로그가 "이미 탈퇴한 회원입니다. 콘텐츠만 삭제됩니다."로
--      분기되는지 검증. 승인해도 서버는 탈퇴를 no-op으로 넘기고 신고 상태만 전이시킨다.
--      ⚠️ 승인 버튼이 비활성화되면 안 된다 — 콘텐츠 제재는 여전히 유효한 동작이다.
INSERT INTO `report` (`reporter_id`, `target_type`, `target_id`, `reason`, `content`, `status`,
                      `processed_id`, `processed_at`, `created_at`, `updated_at`,
                      `reported_member_id`, `target_parent_id`, `target_title`, `target_snippet`)
SELECT (SELECT member_id FROM member WHERE email = 'user06@ddasoom.com'),
       'MEMBER', m.member_id, 'ABUSE',
       '탈퇴 전 작성한 글에서 반복적으로 시비를 걸었습니다.', 'PENDING', NULL, NULL,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 2 DAY),
       m.member_id, NULL, m.nickname, NULL
FROM `member` m
WHERE m.email = 'left01@ddasoom.com';

-- 3-2) ADMIN 대상 — withdrawByReport가 ADMIN을 예외가 아닌 no-op으로 흘리는지 검증.
--      승인해도 200이 떨어지고 관리자 계정은 살아있어야 한다.
--      (예외를 던졌다면 승인 트랜잭션이 롤백되어 콘텐츠 제재까지 취소된다)
INSERT INTO `report` (`reporter_id`, `target_type`, `target_id`, `reason`, `content`, `status`,
                      `processed_id`, `processed_at`, `created_at`, `updated_at`,
                      `reported_member_id`, `target_parent_id`, `target_title`, `target_snippet`)
SELECT (SELECT member_id FROM member WHERE email = 'user09@ddasoom.com'),
       'MEMBER', m.member_id, 'ETC',
       '관리자가 편파적으로 신고를 처리한다고 생각합니다. (악의적 신고 — 반려 대상)', 'PENDING', NULL, NULL,
       DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 1 DAY),
       m.member_id, NULL, m.nickname, NULL
FROM `member` m
WHERE m.email = 'adminkoo@ddasoom.com';


-- =============================================================
-- 검증 쿼리 (시드 적용 후 수동 확인용 — 실행되지 않음)
-- =============================================================
--
-- ① 백필 누락 확인 — 0건이어야 정상
-- SELECT report_id, target_type, target_id, status
--   FROM report WHERE reported_member_id IS NULL;
--
-- ② 상습범 확인 — hidden03(시비왕)의 '화면표시'가 대상별 카운트보다 커야 한다.
--    회원 기준 카운트를 추가한 이유(대상마다 신고를 분산시키는 케이스)가 여기서 드러난다.
-- SELECT m.nickname,
--        COUNT(*)                    AS 전체,
--        SUM(r.status <> 'REJECTED') AS 화면표시,   -- 반려 제외 = 프론트 노출값
--        SUM(r.status = 'REJECTED')  AS 반려
--   FROM report r JOIN member m ON m.member_id = r.reported_member_id
--  WHERE r.deleted_at IS NULL
--  GROUP BY m.member_id ORDER BY 화면표시 DESC;
--
-- ③ 승인 정합성 확인 — 0건이어야 정상 (승인됐는데 피신고자가 살아있는 행)
-- SELECT r.report_id, m.nickname
--   FROM report r JOIN member m ON m.member_id = r.reported_member_id
--  WHERE r.status = 'APPROVED' AND m.deleted_at IS NULL AND m.role <> 'ADMIN';
--
-- ④ 댓글 신고의 원글 링크 조합 가능 여부 — target_parent_id가 NULL이면 링크 버튼이 뜨지 않는다
-- SELECT report_id, target_id, target_parent_id, target_title
--   FROM report WHERE target_type = 'POST_COMMENT';
--
-- ⑤ 프론트 폴백(스냅샷 없는 구 데이터) 검증이 필요할 때만 임시로 실행.
--    시드에 넣지 않은 이유: 데모 중 "깨진 행"처럼 보인다. 확인 후 ①로 되돌릴 것.
-- UPDATE report SET reported_member_id=NULL, target_parent_id=NULL,
--                   target_title=NULL, target_snippet=NULL, updated_at=updated_at
--  WHERE report_id = (SELECT MIN(report_id) FROM (SELECT report_id FROM report) t);