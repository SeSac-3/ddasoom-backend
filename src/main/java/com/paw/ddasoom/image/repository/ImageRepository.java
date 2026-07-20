package com.paw.ddasoom.image.repository;

import com.paw.ddasoom.image.domain.Image;
import com.paw.ddasoom.image.domain.OwnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * 이미지 조회 레포지토리.
 * <p>
 * <b>쿼리 작성 방침:</b> 이 레포지토리는 파생 쿼리(메서드 이름 기반) 대신
 * {@code @Query}(JPQL)로 작성한다. 조회 조건이 {@code ownerType} + {@code ownerId} +
 * {@code deletedAt IS NULL}에 정렬·{@code IN}·{@code isThumbnail} 플래그까지 얹혀
 * 파생 쿼리로 두면 메서드명이 과도하게 길어져 가독성을 해치기 때문이다.
 * <p>
 * 또한 board 도메인 레포지토리도 {@code @Query} 방식으로 작성되어 있어,
 * <b>도메인 간 일관성</b>을 위해 동일한 방식으로 통일한다.
 * <p>
 * {@code @Query}는 파생 쿼리와 달리 메서드명이 WHERE 절을 보증하지 못하므로,
 * 각 메서드의 조건·용도는 개별 JavaDoc으로 명시한다.
 */
public interface ImageRepository extends JpaRepository<Image, Long> {

    /**
     * 소유자의 활성 이미지 목록을 노출 순서대로 조회한다.
     * <p>
     * 상세 조회 화면에서 본문 이미지를 순서대로 렌더링할 때 사용한다.
     * {@code deletedAt IS NULL} 조건으로 soft delete된 이미지는 제외하며,
     * {@code imageOrder} 오름차순(= attach 시점의 업로드/삽입 순서)으로 정렬한다.
     *
     * @param ownerType 소유자 도메인 타입 (POST, QNA 등)
     * @param ownerId   소유자 식별자
     * @return 활성 이미지 목록 (없으면 빈 리스트)
     */
    @Query("""
        SELECT i 
        FROM Image i
        WHERE i.ownerType = :ownerType
        AND i.ownerId = :ownerId
        AND i.deletedAt IS NULL 
        ORDER BY i.imageOrder ASC 
        """)
    List<Image> findActiveImages(OwnerType ownerType, Long ownerId);

    /**
     * 소유자의 활성 이미지 개수를 센다.
     * <p>
     * {@code attach()} 시 소유자당 장수 제한(IMAGE_COUNT_EXCEEDED) 검증에 사용한다.
     * 업로드 시점엔 소유자가 없어 셀 수 없으므로, 확정 연결 시점에 이 카운트로 초과 여부를 판정한다.
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerId   소유자 식별자
     * @return 활성 이미지 개수
     */
    @Query("""
        SELECT COUNT(i)
        FROM Image i
        WHERE i.ownerType = :ownerType
        AND i.ownerId = :ownerId
        AND i.deletedAt IS NULL
        """)
    long countActiveImages(OwnerType ownerType, Long ownerId);


    /**
     * 소유자의 현재 대표 이미지(썸네일) 1건을 조회한다.
     * <p>
     * {@code setThumbnail()}에서 <b>기존 대표 이미지 해제</b> 대상을 찾을 때 사용한다.
     * 소유자당 대표 이미지는 최대 1개이며, 아직 지정 전이면 비어 있을 수 있어 {@link Optional}로 반환한다.
     * (첫 지정 시엔 해제 대상이 없으므로 {@code ifPresent}로 처리하는 것을 전제로 한다.)
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerId   소유자 식별자
     * @return 대표 이미지 (없으면 {@code Optional.empty()})
     */
    @Query("""
        SELECT i
        FROM Image i
        WHERE i.ownerType = :ownerType
        AND i.ownerId = :ownerId
        AND i.isThumbnail IS TRUE 
        AND i.deletedAt IS NULL 
        """)
    Optional<Image> findThumbnail(OwnerType ownerType, Long ownerId);


    /**
     * 여러 소유자의 대표 이미지를 한 번에 조회한다.
     * <p>
     * 목록 페이지에서 각 게시글의 썸네일을 표시할 때, 소유자별로 개별 조회하면 N+1이 발생한다.
     * {@code ownerId IN :ownerIds}로 일괄 조회해 이를 방지한다.
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerIds  소유자 식별자 목록
     * @return 각 소유자의 대표 이미지 목록 (대표 이미지가 없는 소유자는 결과에서 빠짐)
     */
    @Query("""
        SELECT i
        FROM Image i
        WHERE i.ownerType = :ownerType
        AND i.ownerId IN :ownerIds
        AND i.isThumbnail = TRUE 
        AND i.deletedAt IS NULL 
    """)
    List<Image> findThumbnails(OwnerType ownerType, List<Long> ownerIds);
    /**
     * 여러 소유자의 전체 활성 이미지를 노출 순서대로 일괄 조회한다.
     * <p>
     * QnA 댓글 스레드처럼 한 화면에 여러 소유자의 이미지를 모두 렌더링해야 할 때,
     * 소유자별 개별 조회로 인한 N+1을 방지하기 위해 {@code ownerId IN :ownerIds}로 한 번에 가져온다.
     * 호출부에서 {@code ownerId} 기준으로 그룹핑해 사용하는 것을 전제로 한다.
     *
     * @param ownerType 소유자 도메인 타입
     * @param ownerIds  소유자 식별자 목록
     * @return 활성 이미지 목록 ({@code imageOrder} 오름차순, 없으면 빈 리스트)
     */
    @Query("""
        SELECT i
        FROM Image i
        WHERE i.ownerType = :ownerType
        AND i.ownerId IN :ownerType
        AND i.deletedAt IS NULL
        ORDER BY i.imageOrder ASC 
    """)
    List<Image> findActiveImagesByOwners(OwnerType ownerType, List<Long> ownerIds);
}