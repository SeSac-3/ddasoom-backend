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
  Optional<Foster> findByFosterIdAndUser_IdAndDeletedAtIsNull(Long fosterId, Long userId);
  
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

  /** 같은 유저가 같은 동물에 대해 삭제되지 않고, 거절 상태가 아닌 신청이 있는지 확인 */
  boolean existsByUser_IdAndAnimal_IdAndDeletedAtIsNullAndStatusNot(
    Long userId,
    Long animalId,
    FosterStatus status
  );

  /** 관리자 리스트 조회(상태값/임시보호중/삭제/기간 필터링, 신청일 정렬)
   * ex)
   * status = null //전체 조회
   * status = 'PENDING' //PENDING만 조회
   * activeOnly = true // FOSTERING + EXTENDED만 조회(임시보호중인 상태 모음)
   * includeDeleted = true //softDelete 포함 조회
   * includeDeleted = false //softDelete 미포함 조회
   * startAt/endAt // createdAt 기간 조회
   * 
   * 참고)
   * status와 activeOnly=true를 동시에 보내면 둘다 적용 가능성이 있기에
   * 서비스에서 INVALID_FOSTER_SEARCH_CONDITION 예외 처리
   */
  @Query("""
      select f
      from Foster f
      where (:status is null or f.status = :status)
      and (:activeOnly = false or f.status in (
      com.paw.ddasoom.foster.domain.FosterStatus.FOSTERING,
      com.paw.ddasoom.foster.domain.FosterStatus.EXTENDED))
      and (:includeDeleted = true or f.deletedAt is null)
      and (:startAt is null or f.createdAt >= :startAt)
      and (:endAt is null or f.createdAt < :endAt)
      order by f.createdAt desc
      """)
    Page<Foster> findAllForAdmin(
      @Param("status") FosterStatus status,
      @Param("activeOnly") boolean activeOnly ,
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
