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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 이미지 도메인의 <b>팀원용 공개 API</b>. 다른 도메인(board, qna 등)은 이미지 처리를 위해
 * {@code ImageRepository}나 {@code MinioUtil}을 직접 다루지 않고 이 서비스만 호출한다.
 * <p>
 * <b>설계 원칙 (변경 시 IMAGE_FLOW.md와 함께 갱신):</b>
 * <ul>
 *   <li><b>임시 → 확정 2단계</b>: 업로드({@link #upload}) 시점엔 소유자가 없어 {@code owner_id NULL}로
 *       임시 저장하고, 소유자 저장 시점에 {@link #attach}로 확정 연결한다. 하이브리드 업로드 플로우의 근간.</li>
 *   <li><b>MinIO → DB 순서 고정</b>: 업로드는 항상 MinIO 먼저, DB INSERT 나중. 반대면 "DB에 있는데
 *       파일이 없는" 조회 깨짐이 가능하므로, 고아 객체(파일만 남음)를 감수하는 쪽을 택했다.</li>
 *   <li><b>장수 제한은 {@code attach}에서 검증</b>: 업로드 시점엔 소유자가 없어 "소유자당 N장"을 셀 수 없다.
 *       확정 연결 시점에만 소유자 기준 개수를 셀 수 있으므로 검증도 그때 한다.</li>
 *   <li><b>대표 이미지는 사용자 명시적 지정</b>: 백엔드가 자동으로 승격하지 않는다. 썸네일이 삭제돼
 *       사라져도 다른 이미지를 대표로 올리지 않고, 프론트가 재지정을 유도한다.</li>
 *   <li><b>soft delete만 사용</b>: 이미지 삭제는 {@code deletedAt} 세팅뿐. MinIO 객체는 물리 삭제하지
 *       않고 잔존시킨다(고아 수용 — IMAGE_FLOW 7장).</li>
 * </ul>
 * <b>트랜잭션:</b> 쓰기 메서드는 {@code @Transactional}, 조회 전용은 {@code @Transactional(readOnly = true)}.
 * 리치 도메인 메서드({@code attachTo}, {@code softDelete} 등)의 상태 변경은 dirty checking으로 커밋되므로
 * 별도 {@code save()} 호출이 없다.
 */
@Service
@RequiredArgsConstructor
public class ImageService {

    // 모바일 원본 사진(3~8MB) 대응 — Spring multipart 제한(11MB)보다 작아야 이 검증이 먼저 걸림 (IMAGE_FLOW 4장)
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    // 확장자/Content-Type 화이트리스트 — 속이는 경로가 달라 이중 검증 (파일명은 사용자 통제, 헤더는 클라이언트 신고값)
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");

    // 소유자당 최대 장수 — 20장으로 팀 협의 확정 (게시글 UX 및 스토리지 관리 기준).
    // ⚠️ IMAGE_FLOW.md 3-3에 10장으로 남아 있는 서술은 개정 전 값 — 실제 기준은 이 상수(20)
    private static final int MAX_IMAGE_COUNT = 20;

    private final ImageRepository imageRepository;
    private final MinioUtil minioUtil;



    /**
     * 파일을 업로드하고 임시 상태(owner_id NULL)로 저장한다. 소유자 연결은 {@link #attach}에서.
     *
     * <p>순서 고정: 검증 → MinIO → DB INSERT — MinIO 실패 시 DB에 흔적을 남기지 않기 위함.
     * (반대 순서면 "DB에 있는데 파일이 없는" 조회 깨짐 상태 가능 — IMAGE_FLOW 부록 1)
     *
     * <p>업로더(uploaderId)를 함께 기록한다 — owner_id가 NULL인 동안 이 이미지를 확정 연결할 수 있는
     * 유일한 사람을 못박기 위함. 검증은 {@link #attach} 시점에 이루어진다.
     *
     * @param file       업로드할 이미지 파일 (jpeg/png/gif/webp, 10MB 이하)
     * @param ownerType  소유자 도메인 타입 — 버킷/URL 방식 분기의 기준
     * @param uploaderId 업로드한 회원 PK (토큰에서 추출한 값)
     * @return 저장된 이미지의 imageId·URL·썸네일 여부를 담은 응답
     * @throws ImageException IMAGE_001 — 허용 외 형식 / IMAGE_002 — 10MB 초과 / IMAGE_005 — MinIO 실패
     */
    @Transactional
    public ImageResponse upload(MultipartFile file, OwnerType ownerType, Long uploaderId) {
        validateFile(file);

        String objectKey = minioUtil.upload(file, ownerType);

        Image image = Image.builder()
                .ownerType(ownerType)
                .memberId(uploaderId)
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
     * 순서 변경은 재정렬된 리스트로 {@link #syncImages}를 재호출하면 된다 (별도 재정렬 API 없음, IMAGE_FLOW 3-6).
     * 이미지 없는 게시글이 정상 경로이므로, null/빈 리스트는 예외가 아니라 조기 종료로 처리한다.
     *
     * <p>⚠️ {@code requesterId}는 <b>토큰에서 추출한 값만</b> 넘긴다. 요청 body의 회원 ID를 넘기면
     * 업로더 검증이 무력화된다 (SECURITY-FLOW 4장 사용 규칙 4).
     *
     * @param imageIds    연결할 이미지 ID 목록 (순서 = image_order). null/빈 리스트 허용
     * @param ownerType   소유자 도메인 타입
     * @param ownerId     확정된 소유자 식별자 (postId·qnaId 등 — 회원 PK가 아님)
     * @param requesterId 지금 요청한 회원 PK — 임시 이미지의 업로더 대조용
     * @throws ImageException IMAGE_004 — 없거나 삭제된 imageId 포함
     * @throws ImageException IMAGE_006 — 다른 소유자에 연결된 imageId / 업로더가 아닌 사람의 확정 연결
     * @throws ImageException IMAGE_003 — 연결 후 활성 이미지 20장 초과 (전체 롤백)
     */
    @Transactional
    public void attach(List<Long> imageIds, OwnerType ownerType, Long ownerId, Long requesterId) {
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
            image.attachTo(ownerType, ownerId, requesterId);   // 삭제/소유자/업로더 검증은 엔티티가 (IMAGE_004/006)
            image.updateOrder(i);
        }

        // 장수 검증이 연결 "후"인 이유: 업로드 시점엔 소유자가 없어 소유자당 개수를 셀 수 없음.
        // 초과 시 예외 → @Transactional 롤백으로 attachTo/updateOrder까지 전부 취소
        long activeCount = imageRepository.countActiveImages(ownerType, ownerId);
        if (activeCount > MAX_IMAGE_COUNT) {
            throw new ImageException(ImageErrorCode.IMAGE_COUNT_EXCEEDED);
        }
    }

    /**
     * 수정 시 이미지 목록을 요청 상태로 동기화한다. (게시글 수정 시 호출)
     *
     * <p>diff 방식: 기존 활성 중 요청에 없는 것 → soft delete, 나머지는 {@link #attach} 재사용.
     * 소유자 삭제 시 이미지 정리는 imageIds에 null/빈 리스트를 넘기면 된다 (= 전부 삭제).
     *
     * <p>⚠️ 썸네일 이미지가 요청에서 빠져 삭제되면 대표 이미지가 사라진 상태가 되는데,
     * 백엔드가 다른 이미지를 자동 승격하지 않는다 — 대표 이미지는 사용자 명시적 지정 원칙 (IMAGE_FLOW 3-6).
     * 프론트가 재지정을 유도해야 한다.
     *
     * <p>기존에 이미 연결된 이미지는 업로더 검증을 타지 않는다 (엔티티 {@code attachTo} 참고) —
     * 관리자가 남의 글을 수정·삭제하는 정당한 경로가 막히지 않는 이유.
     *
     * @param imageIds    동기화 후 남길 이미지 ID 목록 (순서 = image_order). null/빈 리스트 = 전부 삭제
     * @param ownerType   소유자 도메인 타입
     * @param ownerId     소유자 식별자
     * @param requesterId 지금 요청한 회원 PK — 새로 추가되는 임시 이미지의 업로더 대조용
     * @throws ImageException {@link #attach}와 동일 (IMAGE_003 / 004 / 006)
     */
    @Transactional
    public void syncImages(List<Long> imageIds, OwnerType ownerType, Long ownerId, Long requesterId) {
        // null = 전부 삭제 요청과 동일 — 빈 리스트로 치환하면 아래 로직이 자연스럽게 전부 삭제로 동작
        List<Long> requestedIds = imageIds != null ? imageIds : List.of();

        List<Image> activeImages = imageRepository
                .findActiveImages(ownerType, ownerId);

        // 기존 활성 중 요청에 없는 것만 soft delete (MinIO 객체는 잔존 — 고아 수용, IMAGE_FLOW 7장)
        for (Image image : activeImages) {
            if (!requestedIds.contains(image.getImageId())) {
                image.softDelete();
            }
        }

        // 이미 연결된 이미지가 섞여 있어도 안전 — attachTo()의 "같은 소유자 재연결 no-op" 덕분.
        // 재연결 시에도 updateOrder는 수행되므로, 재정렬된 리스트를 보내면 순서 변경이 이 호출만으로 처리됨
        attach(requestedIds, ownerType, ownerId, requesterId);
    }

    /**
     * 소유자의 활성 이미지 목록을 URL 포함 응답으로 반환한다. (게시글 상세 조회 시 호출)
     * image_order 오름차순 — 본문 삽입/저장 순서 그대로.
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerId   소유자 식별자
     * @return 활성 이미지 응답 목록 (없으면 빈 리스트)
     */
    @Transactional(readOnly = true)
    public List<ImageResponse> getImages(OwnerType ownerType, Long ownerId) {
        return imageRepository
                .findActiveImages(ownerType, ownerId)
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
     * 소유자당 대표는 최대 1개이며, 이 "1개 보장"은 기존 대표를 해제한 뒤 신규를 지정하는 순서로
     * 애플리케이션이 유지한다 (MySQL은 partial unique index 미지원).
     *
     * @param imageId   대표로 지정할 이미지 ID
     * @param ownerType 소유자 도메인 타입
     * @param ownerId   소유자 식별자
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

        imageRepository.findThumbnail(ownerType, ownerId)
                .ifPresent(Image::unmarkAsThumbnail);

        target.markAsThumbnail();
    }

    /**
     * 여러 소유자의 대표 이미지 URL을 배치 조회한다. — 목록 화면 전용.
     *
     * <p>게시글 목록에서 각 글의 썸네일을 표시할 때, 소유자별로 개별 조회하면 N+1이 발생하므로
     * 게시글 수와 무관하게 쿼리 1번으로 가져온다. 썸네일이 없는 소유자는 반환 Map에 키 자체가 없어,
     * 호출부의 {@code get()}이 null을 반환한다 (프론트가 기본 이미지로 처리).
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerIds  소유자 식별자 목록. null/빈 리스트면 빈 Map 반환(불필요한 쿼리 방지)
     * @return {소유자 ID → 썸네일 URL} 맵 (썸네일 없는 소유자는 키 없음)
     */
    @Transactional(readOnly = true)
    public Map<Long, String> getThumbnailUrls(OwnerType ownerType, List<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();  // 빈 페이지 조회 시 불필요한 쿼리 방지 (attach의 조기 return과 같은 방어)
        }

        List<Image> thumbnails = imageRepository
                .findThumbnails(ownerType, ownerIds);

        return thumbnails.stream()
                .collect(Collectors.toMap(
                        Image::getOwnerId,
                        image -> minioUtil.getUrl(ownerType, image.getImageKey())
                ));
    }

    /**
     * 여러 소유자의 <b>전체 활성 이미지</b>를 소유자별로 묶어 배치 조회한다. — QnA 코멘트 스레드 등
     * 한 화면에 여러 소유자의 이미지를 모두 렌더링하는 목록성 화면 전용.
     *
     * <p>소유자별 개별 조회로 인한 N+1을 방지하기 위해 게시글 수와 무관하게 쿼리 1번으로 가져온다.
     * 각 소유자 내부는 image_order 오름차순({@code findActiveImagesByOwners}의 정렬)을 유지하며,
     * {@code LinkedHashMap}으로 그룹핑해 삽입 순서도 보존한다. 이미지가 없는 소유자는 Map에 키가 없어,
     * 호출부는 {@code getOrDefault(id, List.of())}로 처리한다.
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerIds  소유자 식별자 목록. null/빈 리스트면 빈 Map 반환(불필요한 쿼리 방지)
     * @return {소유자 ID → 활성 이미지 응답 목록} 맵 (이미지 없는 소유자는 키 없음)
     */
    @Transactional(readOnly = true)
    public Map<Long, List<ImageResponse>> getImagesGroupedByOwners(OwnerType ownerType, List<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) {
            return Map.of();  // 빈 스레드 조회 시 불필요한 쿼리 방지 (attach의 조기 return과 같은 방어)
        }

        List<Image> images = imageRepository
                .findActiveImagesByOwners(ownerType, ownerIds);

        return images.stream()
                .collect(Collectors.groupingBy(
                        Image::getOwnerId,
                        LinkedHashMap::new,
                        Collectors.mapping(
                                image -> ImageResponse.from(image, minioUtil.getUrl(ownerType, image.getImageKey())),
                                Collectors.toList())));
    }
}