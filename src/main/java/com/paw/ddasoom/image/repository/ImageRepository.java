package com.paw.ddasoom.image.repository;

import com.paw.ddasoom.image.domain.Image;
import com.paw.ddasoom.image.domain.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ImageRepository extends JpaRepository<Image, Long> {

    // 소유자의 활성 이미지 목록 — image_order 오름차순 (상세 조회용)
    List<Image> findAllByOwnerTypeAndOwnerIdAndDeletedAtIsNullOrderByImageOrderAsc(
            OwnerType ownerType, Long ownerId);

    // 소유자의 활성 이미지 수 — attach 시 10장 제한 검증용
    long countByOwnerTypeAndOwnerIdAndDeletedAtIsNull(OwnerType ownerType, Long ownerId);

    // 기존 대표 이미지 1건 — setThumbnail에서 해제 대상 조회용
    Optional<Image> findByOwnerTypeAndOwnerIdAndIsThumbnailTrueAndDeletedAtIsNull(
            OwnerType ownerType, Long ownerId);

    // 여러 소유자의 대표 이미지 일괄 조회 — 목록 페이지 N+1 방지용 (ownerId IN 절)
    List<Image> findAllByOwnerTypeAndOwnerIdInAndIsThumbnailTrueAndDeletedAtIsNull(
            OwnerType ownerType, List<Long> ownerIds);

    // 여러 소유자의 전체 활성 이미지 일괄 조회 - QnA 코멘트 스레드 등 N+1 방지용 (ownerId IN 절)
    List<Image> findAllByOwnerTypeAndOwnerIdInAndDeletedAtIsNullOrderByImageOrderAsc(
            OwnerType ownerType, List<Long> ownerIds);
}