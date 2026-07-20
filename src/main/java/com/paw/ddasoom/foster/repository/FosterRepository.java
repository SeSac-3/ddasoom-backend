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

   /** 관리자 리스트 조회(상태값/임시보호중/삭제/기간 필터링, 신청일 정렬)
    * ex)
    * status = null, activeOnly = false // 전체 조회
    * status = PENDING, activeOnly = false // PENDING 상태만 조회
    * status = null, activeOnly = true // activeStatuses(FOSTERING, EXTENDED)만 조회
    * includeDeleted = true // soft delete된 신청도 포함
    * startAt/endAt // createdAt 기준 기간 조회, endAt은 미포함
    *
    * 참고)
    * activeStatuses는 외부 요청값이 아닌 서비스의 ACTIVE_FOSTER_STATUSES 상수를 전달받는다.
    * status와 activeOnly=true를 동시에 요청하면 조건 충돌 방지를 위해
    * 서비스에서 FOSTER_005 예외 처리한다.
   */
  @Query("""
    select f
    from Foster f
    where (:status is null or f.status = :status)
      and (:activeOnly = false or f.status in :activeStatuses)
      and (:includeDeleted = true or f.deletedAt is null)
      and (:startAt is null or f.createdAt >= :startAt)
      and (:endAt is null or f.createdAt < :endAt)
    order by f.createdAt desc
    """)
Page<Foster> findAllForAdmin(
    @Param("status") FosterStatus status,
    @Param("activeOnly") boolean activeOnly,
    @Param("activeStatuses") Collection<FosterStatus> activeStatuses,
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
