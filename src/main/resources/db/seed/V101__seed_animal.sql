-- =============================================================
-- V101__seed_animal.sql  [개발용 더미 — 전면 리빌드 2026-07]
-- 로컬/테스트 전용. fresh 마이그레이션 전제.
--
-- 통계 검증 목표:
--  * 종별 도넛: 개(D) ≈ 2/3, 고양이(C) ≈ 1/3 비율로 분산 (전체 100마리)
--  * 지역 분포: location 첫 토큰 = 시/도 — 광역 17개 전부에 분산 (SUBSTRING_INDEX 집계 대상)
--  * TOP10: 명시 동물 10마리(이름 식별 가능)가 V103에서 신청을 몰아받아 순위 형성
--  * age/weight는 V2 개정대로 문자열 원본 (SMALLINT/DECIMAL 아님)
--  * animal은 V9에서 deleted_at 드롭 — soft delete 케이스 없음
-- =============================================================

-- ── 1. 명시 동물 10마리 (TOP10 후보 — V103 신청이 이 애들에게 몰림) ──────────
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`, `created_at`, `updated_at`) VALUES
    ('413587202600162', 'D', '뭉치', 'M', '말티즈',       '2024', '서울특별시 강남구',   '3.2',  '흰색',   '오른쪽 귀 접힘',   '접종완료',  NULL, 0, FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 50 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 49 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 49 DAY)),
    ('469569202600557', 'C', '나비', 'F', '코리안숏헤어', '2023', '경기도 수원시',       '3.8',  '고등어', '사람 잘 따름',     '접종완료',  NULL, 0, FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 48 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 47 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 47 DAY)),
    ('450650202601690', 'D', '초코', 'F', '믹스견',       '2022', '부산광역시 해운대구', '12.5', '갈색',   '경계심 많음',      '접종 안함', NULL, 0, TRUE,  DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 46 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 45 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 45 DAY)),
    ('441323202600881', 'D', '보리', 'M', '진돗개',       '2021', '전라남도 목포시',     '15.0', '황색',   '활발함',           '접종완료',  NULL, 0, TRUE,  DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 44 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 43 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 43 DAY)),
    ('427701202600233', 'C', '치즈', 'M', '치즈태비',     '2024', '인천광역시 남동구',   '2.9',  '치즈',   '겁이 많음',        '접종 안함', NULL, 0, FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 42 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 41 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 41 DAY)),
    ('435812202601104', 'D', '해피', 'F', '푸들',         '2023', '대구광역시 수성구',   '4.1',  '베이지', '왼쪽 뒷다리 절음', '접종완료',  NULL, 0, FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 40 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 39 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 39 DAY)),
    ('448290202600412', 'C', '까망', 'F', '봄베이',       '2022', '광주광역시 북구',     '3.5',  '검정',   '없음',             '접종완료',  NULL, 0, TRUE,  DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 38 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 37 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 37 DAY)),
    ('452117202600790', 'D', '탄이', 'M', '리트리버',     '2020', '충청남도 천안시',     '24.3', '골드',   '온순함',           '접종완료',  NULL, 0, TRUE,  DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 36 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 35 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 35 DAY)),
    ('439004202601377', 'C', '구름', 'F', '터키시앙고라', '2023', '제주특별자치도 제주시', '3.1', '흰색',  '장모종',           '접종 안함', NULL, 0, FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 34 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 33 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 33 DAY)),
    ('444561202600925', 'D', '밤톨', 'M', '시바견',       '2024', '경상북도 포항시',     '8.7',  '적갈색', '분리불안 있음',    '접종완료',  NULL, 0, FALSE, DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 32 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 31 DAY), DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL 31 DAY));

-- ── 2. 대량 동물 90마리 — 시/도 17개 순환 + 종 2:1 분산 ────────────────────
-- kind: n MOD 3 → 0,1이면 D(개), 2면 C(고양이) ≈ 2:1
-- location: 시/도 17개를 n MOD 17로 순환 → 전 지역에 5~6마리씩 (지역 분포 차트가 꽉 참)
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `image_url`, `like_count`, `is_fostered`, `rescued_at`, `created_at`, `updated_at`)
WITH RECURSIVE seq(n) AS (
    SELECT 1 UNION ALL SELECT n + 1 FROM seq WHERE n < 90
)
SELECT
    CONCAT('49', LPAD(n, 4, '0'), '2026', LPAD(n * 7 MOD 10000, 5, '0')),   -- 유기번호 유니크 패턴
    IF(n MOD 3 = 2, 'C', 'D'),
    CONCAT('더미동물', n),
    ELT((n MOD 3) + 1, 'M', 'F', 'Q'),
    IF(n MOD 3 = 2,
       ELT((n MOD 4) + 1, '코리안숏헤어', '러시안블루', '페르시안', '샴'),
       ELT((n MOD 5) + 1, '믹스견', '말티즈', '푸들', '진돗개', '포메라니안')),
    CAST(2019 + (n MOD 7) AS CHAR),                                          -- 출생연도 2019~2025 문자열
    CONCAT(
        ELT((n MOD 17) + 1,
            '서울특별시', '부산광역시', '대구광역시', '인천광역시', '광주광역시',
            '대전광역시', '울산광역시', '세종특별자치시', '경기도', '강원특별자치도',
            '충청북도', '충청남도', '전북특별자치도', '전라남도', '경상북도',
            '경상남도', '제주특별자치도'),
        ' ', ELT((n MOD 4) + 1, '중앙동', '행복구', '초록시', '푸른군')),
    CAST(ROUND(2 + (n MOD 20) * 0.7, 1) AS CHAR),                            -- 몸무게 2.0~15.3 문자열
    ELT((n MOD 5) + 1, '흰색', '검정', '갈색', '고등어', '삼색'),
    NULL,
    0, FALSE,
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (30 + (n MOD 150)) DAY),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (29 + (n MOD 150)) DAY),
    DATE_SUB(CURRENT_TIMESTAMP(6), INTERVAL (29 + (n MOD 150)) DAY)
FROM seq;
