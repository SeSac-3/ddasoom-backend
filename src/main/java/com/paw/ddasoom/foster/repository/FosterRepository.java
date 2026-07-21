package com.paw.ddasoom.foster.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;

public interface FosterRepository extends JpaRepository<Foster, Long> {

  /** 유저 수정 조회(유저 본인 + fosterId 조회)*/
  Optional<Foster> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);
  
  /** 유저 리스트 조회( 유저ID/상태값 필터링, 삭제 미포함, 신청일 정렬)
   * ex)
   * status = null //전체 조회
   * status = 'PENDING' //PENDING만 조회
   */
  @Query("""
      select f
      from Foster f
      where f.user.id = :memberId
        and f.deletedAt is null
        and (:status is null or f.status = :status)
      order by f.createdAt desc
      """)
  Page<Foster> findAllForUser(
    @Param("memberId") Long memberId,
    @Param("status") FosterStatus status,
    Pageable pageable
  );

  /** 같은 유저가 같은 동물에 대해 삭제되지 않은 진행 중 신청이 있는지 확인 */
  boolean existsByUser_IdAndAnimal_IdAndDeletedAtIsNullAndStatusIn(
    Long userId,
    Long animalId,
    Collection<FosterStatus> statuses
  );

   /**
   * 관리자 임시보호 목록 조회.
   *
   * statuses는 서비스에서 관리 화면과 상태 토글에 맞춰 결정한다.
   *
   * 예시:
   * - 신청 관리 전체: PENDING, REJECTED
   * - 진행 관리 전체: FOSTERING, EXTENDED, ENDED
   * - 임시보호 중: FOSTERING, EXTENDED
   * - 종료: ENDED
   *
   * endAt은 createdAt 조회 범위에서 미포함 값이다.
   */
  @Query("""
      select f
      from Foster f
      where f.status in :statuses
        and (:includeDeleted = true or f.deletedAt is null)
        and (:startAt is null or f.createdAt >= :startAt)
        and (:endAt is null or f.createdAt < :endAt)
      order by f.createdAt desc
      """)
  Page<Foster> findAllForAdmin(
      @Param("statuses") Collection<FosterStatus> statuses,
      @Param("includeDeleted") boolean includeDeleted,
      @Param("startAt") LocalDateTime startAt,
      @Param("endAt") LocalDateTime endAt,
      Pageable pageable
  );

  /**  관리자 수정 - 해당 동물이 임시보호 상태(FOSTERING/EXTENDED)가 있는지 확인(삭제된 데이터 제외) */
  boolean existsByAnimal_IdAndStatusInAndDeletedAtIsNull(
    Long animalId,
    Collection<FosterStatus> statuses //여러 임시보호 상태를 한번에 IN 조회하기위해 Collection사용
  );

}
