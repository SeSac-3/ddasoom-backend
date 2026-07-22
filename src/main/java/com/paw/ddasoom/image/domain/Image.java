package com.paw.ddasoom.image.domain;

import com.paw.ddasoom.common.util.BaseTimeEntity;
import com.paw.ddasoom.image.exception.ImageErrorCode;
import com.paw.ddasoom.image.exception.ImageException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Getter
@Entity
@Table(name = "image",
        indexes = @Index(name = "idx_image_owner", columnList = "owner_type, owner_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Image extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "image_id", nullable = false)
    private Long imageId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, length = 30)
    private OwnerType ownerType;

    @Column(name = "owner_id")
    private Long ownerId;

    /**
     * 업로더 회원 PK — 임시 이미지(owner_id NULL) 탈취 방지용.
     *
     * <p>연관관계(@ManyToOne)가 아닌 plain Long인 이유: 비교에만 쓰는 값이라 Member 객체가 필요 없고,
     * 연관으로 매핑하면 image 도메인이 member 리포지토리에 의존하게 된다.
     * 같은 엔티티의 {@code ownerId}와 스타일도 일치. (DB 레벨 FK는 fk_image_member로 존재)
     *
     * <p>NULL = V13 이전 레거시 업로드분 — {@link #attachTo}에서 확정 연결이 거부된다.
     */
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "image_key", nullable = false)
    private String imageKey;

    @Column(name = "original_file_name", nullable = false)
    private String originalFileName;

    @Column(name = "file_size", nullable = false, columnDefinition = "BIGINT UNSIGNED")
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "image_order", nullable = false, columnDefinition = "INT UNSIGNED")
    private int imageOrder;

    @Column(name = "is_thumbnail", nullable = false)
    private boolean isThumbnail;

    @Column(name = "deleted_at", columnDefinition = "DATETIME(6)")
    private LocalDateTime deletedAt;

    @Builder
    public Image(OwnerType ownerType, Long ownerId, Long memberId, String imageKey,
                 String originalFileName, Long fileSize, String mimeType) {
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.memberId = memberId;
        this.imageKey = imageKey;
        this.originalFileName = originalFileName;
        this.fileSize = fileSize;
        this.mimeType = mimeType;
    }

    // ========== 리치 도메인 메서드 (상태 변경은 아래 메서드로만) ==========

    /**
     * 소유자에 확정 연결한다. (게시글/문의 저장 시점에 ImageService.attach()가 호출)
     *
     * <p>같은 소유자로의 재연결은 no-op — syncImages()가 diff 없이
     * 전체 리스트로 재호출해도 안전한 이유가 이 분기다.
     *
     * @param requesterId 지금 요청한 회원 PK — 임시 이미지의 최초 연결에만 업로더와 대조한다
     * @throws ImageException IMAGE_004 — 삭제된 이미지
     * @throws ImageException IMAGE_006 — ownerType 불일치 / 이미 다른 소유자에 연결됨 / 업로더가 아닌 사람의 확정 연결
     */
    public void attachTo(OwnerType ownerType, Long ownerId, Long requesterId) {
        validateNotDeleted();

        boolean isAttachedToOther = this.ownerId != null && !this.ownerId.equals(ownerId);
        if (this.ownerType != ownerType || isAttachedToOther) {
            throw new ImageException(ImageErrorCode.IMAGE_OWNER_MISMATCH);
        }

        // 임시 이미지(owner_id NULL)의 "최초" 확정 연결에만 업로더를 검증한다.
        // imageId를 추측해 남이 방금 올린 이미지를 자기 글에 붙이는 탈취 벡터를 여기서 끊는다.
        //
        // 이미 연결된 이미지의 재연결(syncImages 경로)은 위 소유자 검증이 이미 담당하므로 제외 —
        // 관리자가 남의 글을 수정/삭제하는 정당한 경로까지 막지 않기 위함.
        // 업로더 NULL(V13 이전 레거시)도 대조 불가라 거부 — 확정 연결을 막는 쪽이 안전한 실패.
        boolean isUploaderMismatch = this.memberId == null || !this.memberId.equals(requesterId);
        if (this.ownerId == null && isUploaderMismatch) {
            throw new ImageException(ImageErrorCode.IMAGE_OWNER_MISMATCH);
        }

        this.ownerId = ownerId;
    }

    /**
     * soft delete — 물리 삭제 금지 (DB 컨벤션 5장). MinIO 객체는 삭제하지 않는다 (고아 객체 수용 — IMAGE_FLOW 7장)
     */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * 대표 이미지로 지정한다. 소유자당 1개 보장은 ImageService.setThumbnail()이
     * 기존 대표를 unmark하는 방식으로 책임진다 — 엔티티는 자기 상태만 검증.
     *
     * @throws ImageException IMAGE_004 — 삭제된 이미지
     */
    public void markAsThumbnail() {
        validateNotDeleted();
        this.isThumbnail = true;
    }

    public void unmarkAsThumbnail() {
        this.isThumbnail = false;
    }

    /**
     * 노출 순서 갱신 — attach() 호출마다 요청 리스트의 인덱스로 매번 갱신된다.
     * 재연결이어도 갱신하므로, 드래그 재정렬은 재정렬된 imageIds를 syncImages()로 재전송하면 끝.
     */
    public void updateOrder(int order) {
        this.imageOrder = order;
    }

    // 삭제된 이미지는 어떤 상태 변경도 불가 — attachTo / markAsThumbnail 공통 가드
    private void validateNotDeleted() {
        if (this.deletedAt != null) {
            throw new ImageException(ImageErrorCode.IMAGE_NOT_FOUND);
        }
    }

}