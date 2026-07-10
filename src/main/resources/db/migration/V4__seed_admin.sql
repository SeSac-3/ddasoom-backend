-- =============================================================
-- [운영 필수 시드] ADMIN 계정
-- =============================================================

SET @pw := '$2b$10$nFgxKiqsJPXAW3uWp5/tqemYRrE1LUkzhftrV4GvrYVJ3r2XF9VN2';

INSERT INTO `member` (`email`, `password`, `name`, `nickname`, `tel`, `role`, `created_at`, `updated_at`) VALUES
                                                                                                              ('adminkoo@ddasoom.com',    @pw, '구지훈', '관리자지훈', '01011110001', 'ADMIN', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                                                                                                              ('adminlee@ddasoom.com',    @pw, '이서진', '관리자서진', '01011110002', 'ADMIN', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                                                                                                              ('adminyu@ddasoom.com', @pw, '유창호', '관리자창호', '01011110003', 'ADMIN', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                                                                                                              ('adminkim@ddasoom.com',     @pw, '김경우', '관리자경우', '01011110004', 'ADMIN', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)),
                                                                                                              ('adminsik@ddasoom.com',     @pw, '김종식', '관리자종식', '01011110005', 'ADMIN', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6));