-- =============================================
-- [개발용 더미] animal (local/test 전용)
-- =============================================

-- [동물-1] 기본 정상 케이스: 개, 수컷, 전 필드 채움, like_count가 실제 animal_like 활성 행 수(2)와 일치
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('413587202600162', 'D', '뭉치', 'M', '말티즈', '2024', '서울특별시 강남구', '3.2', '흰색',
        '오른쪽 귀 접힘', '접종완료', 'http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/07/202607081107865.jpg', 2, FALSE, '2026-06-01 08:00:00.000000',
        '2026-06-02 09:00:00.000000', '2026-06-02 09:00:00.000000', NULL);

-- [동물-2] DB DEFAULT 값 활용 케이스: nickname/gender/special_mark/vaccination_chk 전부 미지정 -> DEFAULT('미정'/'Q'/'없음'/'접종 안함') 적용 확인
INSERT INTO `animal` (`abandonment_id`, `kind`, `type_name`, `age`, `location`, `weight`, `color`,
                      `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('469569202600557', 'C', '코리안숏헤어', '2023', '경기도 수원시', '3.8', '고등어',
        NULL, 0, FALSE, '2026-05-15 07:30:00.000000',
        '2026-05-16 10:00:00.000000', '2026-05-16 10:00:00.000000', NULL);

-- [동물-3] gender=F, is_fostered=TRUE(임시보호 중) 케이스 - 임시보호 필터 쿼리 검증용
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('450650202601690', 'D', '초코', 'F', '믹스견', '2022', '부산광역시 해운대구', '12.5', '갈색',
        '경계심 많음', '접종 안함', 'http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/07/202607082107857.jpeg', 1, TRUE, NULL,
        '2026-04-01 09:00:00.000000', '2026-04-20 09:00:00.000000', NULL);

-- [동물-4] soft-delete 된 동물 - 목록/상세 조회 시 deleted_at IS NULL 필터로 제외되는지 검증용
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('450650202601689', 'C', '나비', 'M', '페르시안', '2021', '인천광역시 남동구', '4.1', '검정색',
        '없음', '접종완료', NULL, 1, TRUE, '2026-03-10 06:00:00.000000',
        '2026-03-11 08:00:00.000000', '2026-06-01 12:00:00.000000', '2026-06-01 12:00:00.000000');

-- [동물-5] age/weight에 공공API 원본 그대로의 지저분한 문자열 저장 - VARCHAR 전환의 핵심 목적(파싱 없이 원본 보존) 검증용
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('448567202601029', 'D', '콩이', 'Q', '푸들', '2026(년생) 추정 2개월', '대구광역시 수성구', '0.3(Kg) 추정', '베이지',
        '포유 중', '접종 안함', 'https://example.com/images/animal5.jpg', 0, FALSE, '2026-06-25 05:00:00.000000',
        '2026-06-25 09:00:00.000000', '2026-06-25 09:00:00.000000', NULL);

-- [동물-6] age/weight 원본이 아예 미상인 극단 케이스 - 빈 값에 가까운 원본 문자열 저장 확인
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('450650202601688', 'C', '미정탐정', 'Q', '한국고양이', '나이 미상(공고문 기재 누락)', '강원특별자치도 춘천시', '체중 미상', '삼색',
        '없음', '접종 안함', NULL, 0, FALSE, NULL,
        '2026-07-01 09:00:00.000000', '2026-07-01 09:00:00.000000', NULL);

-- [동물-7] abandonment_id가 VARCHAR(20) 최대 길이에 근접한 경계값 케이스 (길이 제약 검증용)
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('448567202601027', 'D', '대박이', 'M', '진돗개', '2018', '전라남도 여수시', '45.9', '황색',
        '없음', '접종완료', 'http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/07/202607081607515.jpg', 3, FALSE, '2026-02-14 03:00:00.000000',
        '2026-02-15 07:00:00.000000', '2026-02-15 07:00:00.000000', NULL);

-- [동물-8] nickname/type_name/color가 컬럼 최대 길이(50자)에 꽉 찬 경계값 케이스
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('448545202600355', 'D',
        '가나다라마바사아자차카타파하가나다라마바사아자차카타파하가나다라마바사아', 'F',
        '아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주아주긴품종이름오십자', '2020',
        '충청북도 청주시', '9.4', '갈색과흰색이섞인아주긴색깔이름오십자까지채워보는테스트용문자열입니당', '없음', '접종완료',
        'http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/07/202607081107958.jpg', 1, FALSE, '2026-01-20 04:00:00.000000',
        '2026-01-21 08:00:00.000000', '2026-06-30 08:00:00.000000', NULL);

-- [동물-9] like_count 캐시(5)와 실제 animal_like 활성 행 수(1)가 의도적으로 불일치 - Write-Behind 동기화 배치 검증용
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('448543202600168', 'D', '싱크로', 'F', '비글', '2020', '광주광역시 서구', '9.4', '갈색/흰색',
        '없음', '접종완료', 'http://openapi.animal.go.kr/openapi/service/rest/fileDownloadSrvc/files/shelter/2026/07/202607081107992.jpg', 5, FALSE, '2026-01-20 04:00:00.000000',
        '2026-01-21 08:00:00.000000', '2026-06-30 08:00:00.000000', NULL);

-- [동물-10] 좋아요 0건, image_url NULL, rescued_at NULL - 연관 데이터/선택값이 전부 비어있는 최소 데이터 케이스
INSERT INTO `animal` (`abandonment_id`, `kind`, `nickname`, `gender`, `type_name`, `age`, `location`, `weight`, `color`,
                      `special_mark`, `vaccination_chk`, `image_url`, `like_count`, `is_fostered`, `rescued_at`,
                      `created_at`, `updated_at`, `deleted_at`)
VALUES ('448542202600755', 'C', '민들레', 'F', '터키시앙고라', '2025', '제주특별자치도 제주시', '2.9', '흰색',
        '없음', '접종 안함', NULL, 0, FALSE, NULL,
        '2026-07-05 09:00:00.000000', '2026-07-05 09:00:00.000000', NULL);