-- =============================================================
-- [개발용 더미] 일반 회원 (USER)
-- 로컬/테스트 전용 - 운영 DB에는 포함되지 않음 (db/seed, 방식2)
-- event_status: user01, user02는 이벤트 참여중(PARTICIPATING) 더미로 지정
--   → board 담당자 게시글 수 연동 테스트, 프론트 이벤트 화면 테스트용
-- =============================================================

SET @pw := '$2b$10$nFgxKiqsJPXAW3uWp5/tqemYRrE1LUkzhftrV4GvrYVJ3r2XF9VN2';

INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `event_status`, `created_at`, `updated_at`) VALUES
    ('user01@ddasoom.com', @pw, '박하늘', '하늘이집사', '01022220001', 'USER', 'PARTICIPATING', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('user02@ddasoom.com', @pw, '최바다', '바다냥집사', '01022220002', 'USER', 'PARTICIPATING', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('user03@ddasoom.com', @pw, '정온유', '온유멍집사', '01022220003', 'USER', 'NONE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('user04@ddasoom.com', @pw, '한별님', '별님보호소', '01022220004', 'USER', 'NONE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
    ('user05@ddasoom.com', @pw, '오솜이', '솜이누나',   '01022220005', 'USER', 'NONE', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));