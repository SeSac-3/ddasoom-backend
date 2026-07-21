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

  /**
   * 본인 소유이며 삭제되지 않은 임시보호 신청을 조회한다.
   *
   * <p>타인 신청은 조회되지 않으므로 존재 여부가 외부에 노출되지 않는다.</p>
   *
   * @param id 임시보호 신청 PK
   * @param userId 로그인 사용자 PK
   * @return 본인 소유의 삭제되지 않은 신청
   */
  Optional<Foster> findByIdAndUser_IdAndDeletedAtIsNull(Long id, Long userId);

  /**
   * 사용자의 임시보호 신청 목록을 상태별로 조회한다.
   *
   * <p>
   * 목록 응답에 필요한 동물 정보를 함께 조회해,
   * 신청 행마다 동물을 다시 조회하는 N+1 문제를 방지한다.
   * </p>
   *
   * @param memberId 로그인 사용자 PK
   * @param status 상태 필터. null이면 전체 상태
   * @param pageable 페이지 정보
   * @return 임시보호 신청 페이지
   */
  @Query(
      value = """
          select f
          from Foster f
          join fetch f.animal
          where f.user.id = :memberId
            and f.deletedAt is null
            and (:status is null or f.status = :status)
          order by f.createdAt desc
          """,
      countQuery = """
          select count(f)
          from Foster f
          where f.user.id = :memberId
            and f.deletedAt is null
            and (:status is null or f.status = :status)
          """
  )
  Page<Foster> findAllForUser(
      @Param("memberId") Long memberId,
      @Param("status") FosterStatus status,
      Pageable pageable
  );

  /**
   * 같은 사용자가 같은 동물에 대해 삭제되지 않은 진행 중 신청을 가지고 있는지 확인한다.
   *
   * @param userId 사용자 PK
   * @param animalId 동물 PK
   * @param statuses 중복 신청을 차단할 상태 목록
   * @return 진행 중 신청 존재 여부
   */
  boolean existsByUser_IdAndAnimal_IdAndDeletedAtIsNullAndStatusIn(
      Long userId,
      Long animalId,
      Collection<FosterStatus> statuses
  );

  /**
   * 현재 신청을 제외하고, 같은 동물에 삭제되지 않은 활성 임시보호 신청이 있는지 확인한다.
   *
   * <p>관리자가 신청을 활성 상태로 변경하기 전에 사용한다.</p>
   *
   * @param animalId 동물 PK
   * @param fosterId 현재 수정 중인 임시보호 신청 PK
   * @param statuses 활성 상태 목록
   * @return 다른 활성 신청 존재 여부
   */
  boolean existsByAnimal_IdAndIdNotAndStatusInAndDeletedAtIsNull(
      Long animalId,
      Long fosterId,
      Collection<FosterStatus> statuses
  );

  /**
 * 관리자 임시보호 목록을 관리 범위와 상태 조건으로 조회한다.
 *
 * <p>
 * 목록 응답에서 사용하는 animal, user, reviewer를 한 번에 조회해
 * 행마다 추가 조회가 발생하는 N+1 문제를 방지한다.
 * </p>
 *
 * @param statuses 조회할 상태 목록
 * @param includeDeleted true면 삭제된 신청 포함
 * @param startAt 신청일 조회 시작 시각
 * @param endAt 신청일 조회 종료 시각 미포함
 * @param pageable 페이지 정보
 * @return 관리자용 임시보호 신청 페이지
 */
@Query(
    value = """
        select f
        from Foster f
        join fetch f.animal
        join fetch f.user
        left join fetch f.reviewer
        where f.status in :statuses
          and (:includeDeleted = true or f.deletedAt is null)
          and (:startAt is null or f.createdAt >= :startAt)
          and (:endAt is null or f.createdAt < :endAt)
        order by f.createdAt desc
        """,
    countQuery = """
        select count(f)
        from Foster f
        where f.status in :statuses
          and (:includeDeleted = true or f.deletedAt is null)
          and (:startAt is null or f.createdAt >= :startAt)
          and (:endAt is null or f.createdAt < :endAt)
        """
)
Page<Foster> findAllForAdmin(
    @Param("statuses") Collection<FosterStatus> statuses,
    @Param("includeDeleted") boolean includeDeleted,
    @Param("startAt") LocalDateTime startAt,
    @Param("endAt") LocalDateTime endAt,
    Pageable pageable
);

  /**
   * 해당 동물에 삭제되지 않은 활성 임시보호 신청이 있는지 확인한다.
   *
   * @param animalId 동물 PK
   * @param statuses 활성 상태 목록
   * @return 활성 임시보호 신청 존재 여부
   */
  boolean existsByAnimal_IdAndStatusInAndDeletedAtIsNull(
      Long animalId,
      Collection<FosterStatus> statuses
  );
}