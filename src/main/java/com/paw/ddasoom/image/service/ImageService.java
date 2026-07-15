package com.paw.ddasoom.image.service;

import com.paw.ddasoom.image.domain.Image;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.exception.ImageErrorCode;
import com.paw.ddasoom.image.exception.ImageException;
import com.paw.ddasoom.image.repository.ImageRepository;
import com.paw.ddasoom.image.util.MinioUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ImageService {

    // 모바일 원본 사진(3~8MB) 대응 — Spring multipart 제한(11MB)보다 작아야 이 검증이 먼저 걸림 (IMAGE_FLOW 4장)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 확장자/Content-Type 화이트리스트 — 속이는 경로가 달라 이중 검증 (파일명은 사용자 통제, 헤더는 클라이언트 신고값)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    // 소유자당 최대 장수 — 게시글 UX 및 스토리지 관리 기준 (IMAGE_FLOW 3-3)
    private static final int MAX_IMAGE_COUNT = 10;

    private final ImageRepository imageRepository;
    private final MinioUtil minioUtil;



    /**
     * 파일을 업로드하고 임시 상태(owner_id NULL)로 저장한다. 소유자 연결은 attach()에서.
     *
     * <p>순서 고정: 검증 → MinIO → DB INSERT — MinIO 실패 시 DB에 흔적을 남기지 않기 위함.
     * (반대 순서면 "DB에 있는데 파일이 없는" 조회 깨짐 상태 가능 — IMAGE_FLOW 부록 1)
     *
     * @throws ImageException IMAGE_001 — 허용 외 형식 / IMAGE_002 — 10MB 초과 / IMAGE_005 — MinIO 실패
     */
    @Transactional
    public ImageResponse upload(MultipartFile file, OwnerType ownerType) {
        validateFile(file);

        String objectKey = minioUtil.upload(file, ownerType);

        Image image = Image.builder()
                .ownerType(ownerType)
                .imageKey(objectKey)
                .originalFileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .mimeType(file.getContentType())
                .build();
        imageRepository.save(image);

        String url = minioUtil.getUrl(ownerType, objectKey);
        return ImageResponse.from(image, url);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageException(ImageErrorCode.INVALID_IMAGE_TYPE);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ImageException(ImageErrorCode.IMAGE_SIZE_EXCEEDED);
        }

        String extension = extractExtension(file.getOriginalFilename());
        boolean isInvalidType = !ALLOWED_EXTENSIONS.contains(extension)
                || !ALLOWED_MIME_TYPES.contains(file.getContentType());
        if (isInvalidType) {
            throw new ImageException(ImageErrorCode.INVALID_IMAGE_TYPE);
        }
    }

    // MinioUtil.createObjectKey가 "검증 완료된 확장자"를 전제하는데, 그 전제를 만드는 곳이 여기
    private String extractExtension(String fileName) {
        if (fileName == null || fileName.lastIndexOf('.') == -1) {
            return "";   // 확장자 없음 → 화이트리스트에 없으니 자연히 INVALID_IMAGE_TYPE
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 업로드된 이미지들을 소유자에 확정 연결한다. (게시글/문의 생성 시 호출)
     *
     * <p>imageIds 리스트의 인덱스가 그대로 image_order로 저장된다 —
     * 순서 변경은 재정렬된 리스트로 syncImages()를 재호출하면 된다 (별도 재정렬 API 없음, IMAGE_FLOW 3-6).
     *
     * @throws ImageException IMAGE_004 — 없거나 삭제된 imageId 포함
     * @throws ImageException IMAGE_006 — 다른 소유자에 연결된 imageId 포함
     * @throws ImageException IMAGE_003 — 연결 후 활성 이미지 10장 초과 (전체 롤백)
     */
    @Transactional
    public void attach(List<Long> imageIds, OwnerType ownerType, Long ownerId) {
        // 이미지 없는 게시글이 정상 경로 — 예외가 아니라 조기 종료
        if (imageIds == null || imageIds.isEmpty()) {
            return;
        }

        List<Image> images = imageRepository.findAllById(imageIds);
        if (images.size() != imageIds.size()) {
            throw new ImageException(ImageErrorCode.IMAGE_NOT_FOUND);
        }

        // findAllById는 순서 비보장 → Map 변환 후 "요청 리스트 순서"로 순회해야 image_order가 정확
        Map<Long, Image> imageMap = images.stream()
                .collect(Collectors.toMap(Image::getImageId, Function.identity()));

        for (int i = 0; i < imageIds.size(); i++) {
            Image image = imageMap.get(imageIds.get(i));
            image.attachTo(ownerType, ownerId);   // 삭제/소유자 검증은 엔티티가 (IMAGE_004/006)
            image.updateOrder(i);
        }

        // 장수 검증이 연결 "후"인 이유: 업로드 시점엔 소유자가 없어 소유자당 개수를 셀 수 없음.
        // 초과 시 예외 → @Transactional 롤백으로 attachTo/updateOrder까지 전부 취소
        long activeCount = imageRepository.countByOwnerTypeAndOwnerIdAndDeletedAtIsNull(ownerType, ownerId);
        if (activeCount > MAX_IMAGE_COUNT) {
            throw new ImageException(ImageErrorCode.IMAGE_COUNT_EXCEEDED);
        }
    }

    /**
     * 수정 시 이미지 목록을 요청 상태로 동기화한다. (게시글 수정 시 호출)
     *
     * <p>diff 방식: 기존 활성 중 요청에 없는 것 → soft delete, 나머지는 attach() 재사용.
     * 소유자 삭제 시 이미지 정리는 imageIds에 null/빈 리스트를 넘기면 된다 (= 전부 삭제).
     *
     * <p>⚠️ 썸네일 이미지가 요청에서 빠져 삭제되면 대표 이미지가 사라진 상태가 되는데,
     * 백엔드가 다른 이미지를 자동 승격하지 않는다 — 대표 이미지는 사용자 명시적 지정 원칙 (IMAGE_FLOW 3-6).
     * 프론트가 재지정을 유도해야 한다.
     *
     * @throws ImageException attach()와 동일 (IMAGE_003 / 004 / 006)
     */
    @Transactional
    public void syncImages(List<Long> imageIds, OwnerType ownerType, Long ownerId) {
        // null = 전부 삭제 요청과 동일 — 빈 리스트로 치환하면 아래 로직이 자연스럽게 전부 삭제로 동작
        List<Long> requestedIds = imageIds != null ? imageIds : List.of();

        List<Image> activeImages = imageRepository
                .findAllByOwnerTypeAndOwnerIdAndDeletedAtIsNullOrderByImageOrderAsc(ownerType, ownerId);

        // 기존 활성 중 요청에 없는 것만 soft delete (MinIO 객체는 잔존 — 고아 수용, IMAGE_FLOW 7장)
        for (Image image : activeImages) {
            if (!requestedIds.contains(image.getImageId())) {
                image.softDelete();
            }
        }

        // 이미 연결된 이미지가 섞여 있어도 안전 — attachTo()의 "같은 소유자 재연결 no-op" 덕분.
        // 재연결 시에도 updateOrder는 수행되므로, 재정렬된 리스트를 보내면 순서 변경이 이 호출만으로 처리됨
        attach(requestedIds, ownerType, ownerId);
    }

    /**
     * 소유자의 활성 이미지 목록을 URL 포함 응답으로 반환한다. (게시글 상세 조회 시 호출)
     * image_order 오름차순 — 본문 삽입/저장 순서 그대로.
     */
    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(OwnerType ownerType, Long ownerId) {
        return imageRepository
                .findAllByOwnerTypeAndOwnerIdAndDeletedAtIsNullOrderByImageOrderAsc(ownerType, ownerId)
                .stream()
                .map(image -> ImageResponse.from(image, minioUtil.getUrl(ownerType, image.getImageKey())))
                .toList();
    }

    /**
     * 대표 이미지를 지정한다. (게시글 작성/수정 시 thumbnailImageId로 호출)
     *
     * <p>순서 고정: 검증 → 기존 해제 → 신규 지정.
     * 검증을 먼저 두는 이유 — 대상이 유효하지 않으면 기존 대표를 건드리지 않고 즉시 실패해야
     * (롤백에 기대지 않고) 상태 변경이 최소화된다.
     *
     * @throws ImageException IMAGE_004 — 없거나 삭제된 imageId
     * @throws ImageException IMAGE_006 — 다른 소유자의 imageId
     */
    @Transactional
    public void setThumbnail(Long imageId, OwnerType ownerType, Long ownerId) {
        Image target = imageRepository.findById(imageId)
                .orElseThrow(() -> new ImageException(ImageErrorCode.IMAGE_NOT_FOUND));

        boolean isOwnerMismatch = target.getOwnerType() != ownerType
                || !ownerId.equals(target.getOwnerId());
        if (isOwnerMismatch) {
            throw new ImageException(ImageErrorCode.IMAGE_OWNER_MISMATCH);
        }

        imageRepository.findByOwnerTypeAndOwnerIdAndIsThumbnailTrueAndDeletedAtIsNull(ownerType, ownerId)
                .ifPresent(Image::unmarkAsThumbnail);

        target.markAsThumbnail();
    }

    /**
     * 여러 소유자의 썸네일 URL 배치 조회 — 목록 화면 전용 (게시글 수와 무관하게 쿼리 1번, N+1 방지).
     * 썸네일이 없는 소유자는 Map에 키 자체가 없음 → 호출부의 get()이 null 반환 (프론트가 기본 이미지 처리)
     */
    @Transactional(readOnly = true)
    public Map<Long, String> getThumbnailUrls(OwnerType ownerType, List<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();  // 빈 페이지 조회 시 불필요한 쿼리 방지 (attach의 조기 return과 같은 방어)
        }

        List<Image> thumbnails = imageRepository
                .findAllByOwnerTypeAndOwnerIdInAndIsThumbnailTrueAndDeletedAtIsNull(ownerType, ownerIds);

        return thumbnails.stream()
                .collect(Collectors.toMap(
                        Image::getOwnerId,
                        image -> minioUtil.getUrl(ownerType, image.getImageKey())
                ));
    }

}