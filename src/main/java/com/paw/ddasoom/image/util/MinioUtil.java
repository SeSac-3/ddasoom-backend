package com.paw.ddasoom.image.util;

import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.exception.ImageErrorCode;
import com.paw.ddasoom.image.exception.ImageException;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class MinioUtil {

    private final MinioClient minioClient;

    @Value("${minio.bucket.public-name}")
    private String publicBucket;

    @Value("${minio.bucket.private-name}")
    private String privateBucket;

    /**
     * 파일을 ownerType에 맞는 버킷에 업로드하고 객체 키를 반환한다.
     * 버킷 선택/키 생성은 전부 여기서 — 호출자(ImageService)는 버킷을 몰라야 한다.
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
            try {
                throw new ImageException(ImageErrorCode.IMAGE_UPLOAD_FAILED);
            } catch (ImageException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private String selectBucket(OwnerType ownerType) {
        return ownerType.isPublic() ? publicBucket : privateBucket;
    }

    // 키 형식: {yyyy}/{MM}/{용도}/{uuid}.{확장자} — 기간별 백업/운영 관리 용이 (IMAGE_FLOW 부록 1)
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