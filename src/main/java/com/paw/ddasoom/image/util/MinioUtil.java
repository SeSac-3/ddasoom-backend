package com.paw.ddasoom.image.util;

import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.exception.ImageErrorCode;
import com.paw.ddasoom.image.exception.ImageException;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 접근 유틸 — 버킷 선택, 객체 키 생성, URL 방식 분기를 전부 캡슐화한다.
 * 호출자(ImageService)는 버킷/키 규칙을 몰라야 한다. (IMAGE_FLOW.md 3-4)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    // Presigned URL 만료 시간 — 조회 시점마다 새로 발급하므로 짧게 유지 (IMAGE_FLOW 결정: 30분)
    private static final int PRESIGNED_EXPIRY_MINUTES = 30;

    // 익명 읽기 전용 정책 — Action은 GetObject만 (PutObject 포함 시 익명 업로드가 열림)
    private static final String PUBLIC_READ_POLICY = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Principal": "*",
                  "Action": ["s3:GetObject"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                }
              ]
            }
            """;

    private final MinioClient minioClient;

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket.public-name}")
    private String publicBucket;

    @Value("${minio.bucket.private-name}")
    private String privateBucket;

    /**
     * 기동 시 버킷 2개 생성 + public 버킷 익명 읽기 정책 설정.
     *
     * <p>⚠️ 실패해도 서버는 떠야 한다 — MinIO 미기동 팀원 보호
     * (yml의 Redis autoconfigure 제외와 같은 방어 철학, IMAGE_FLOW 3-4)
     */
    @PostConstruct
    public void initBuckets() {
        try {
            createBucketIfNotExists(publicBucket);
            createBucketIfNotExists(privateBucket);

            minioClient.setBucketPolicy(
                    SetBucketPolicyArgs.builder()
                            .bucket(publicBucket)
                            .config(PUBLIC_READ_POLICY.formatted(publicBucket))
                            .build()
            );
            log.info("MinIO 버킷 초기화 완료 - public: {}, private: {}", publicBucket, privateBucket);
        } catch (Exception e) {
            log.warn("MinIO 버킷 초기화 실패 — 이미지 기능 비활성 상태로 기동. MinIO 기동 후 서버 재시작 필요", e);
        }
    }

    /**
     * 파일을 ownerType에 맞는 버킷에 업로드하고 객체 키를 반환한다.
     *
     * @throws ImageException IMAGE_005 — MinIO 통신 실패
     */
    public String upload(MultipartFile file, OwnerType ownerType) {
        String bucket = selectBucket(ownerType);
        String objectKey = createObjectKey(ownerType, file.getOriginalFilename());

        // try-with-resources — 업로드 성공/실패와 무관하게 스트림 확실히 닫기
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
            return objectKey;
        } catch (Exception e) {
            // MinIO SDK 예외는 checked가 다수 + IO 예외까지 겹쳐 개별 catch 실익 없음 → 일괄 변환 (IMAGE_FLOW 3-4)
            log.error("MinIO 업로드 실패 - bucket: {}, key: {}", bucket, objectKey, e);
            throw new ImageException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    /**
     * 객체 키로 접근 URL을 발급한다. 공개/비공개 분기는 이 메서드가 캡슐화 —
     * 호출자(ImageService)는 URL 방식을 몰라야 한다.
     *
     * <p>공개: 영구 정적 URL (본문에 박히는 URL이라 만료되면 안 됨 — IMAGE_FLOW 부록 1)
     * <p>비공개: Presigned URL 30분 (QNA 개인정보 보호)
     *
     * @throws ImageException IMAGE_005 — Presigned URL 발급 실패
     */
    public String getUrl(OwnerType ownerType, String objectKey) {
        if (ownerType.isPublic()) {
            return "%s/%s/%s".formatted(endpoint, publicBucket, objectKey);
        }
        return createPresignedUrl(objectKey);
    }

    private void createBucketIfNotExists(String bucket) throws Exception {
        boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private String selectBucket(OwnerType ownerType) {
        return ownerType.isPublic() ? publicBucket : privateBucket;
    }

    // 비공개 버킷 전용 — 만료/서명 실패 가능성이 있는 유일한 URL 경로라 try를 여기로 한정
    private String createPresignedUrl(String objectKey) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(privateBucket)
                            .object(objectKey)
                            .expiry(PRESIGNED_EXPIRY_MINUTES, TimeUnit.MINUTES)
                            .build()
            );
        } catch (Exception e) {
            log.error("Presigned URL 발급 실패 - key: {}", objectKey, e);
            throw new ImageException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
        }
    }

    // 키 형식: {yyyy}/{MM}/{용도}/{uuid}.{확장자} — 기간별 백업/운영 관리 용이 (IMAGE_FLOW 부록 1)
    // 확장자 검증은 ImageService의 파일 검증에서 upload 호출 전에 완료됨을 전제로 함
    private String createObjectKey(OwnerType ownerType, String originalFileName) {
        LocalDate now = LocalDate.now();
        String extension = extractExtension(originalFileName);

        return "%d/%02d/%s/%s.%s".formatted(
                now.getYear(),
                now.getMonthValue(),
                ownerType.name().toLowerCase(),
                UUID.randomUUID(),
                extension
        );
    }

    private String extractExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}