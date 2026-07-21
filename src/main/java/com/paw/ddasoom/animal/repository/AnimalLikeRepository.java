package com.paw.ddasoom.animal.repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.domain.AnimalLike;

/**
 * animal_like 읽기 전용 레포지토리.
 * 쓰기(insert/delete/카운트 갱신)는 Write-Behind 배치의 AnimalLikeJdbcRepository가 담당하고,
 * 여기서는 "내가 좋아요했는지 / 내가 좋아요한 동물 목록" 같은 조회만 다룬다.
 * ⚠️ Redis dirty(미flush분)는 반영하지 않는 커밋 상태 기준 — 최대 flush 주기(약 10초)만큼 지연될 수 있다.
 */
public interface AnimalLikeRepository extends JpaRepository<AnimalLike, Long> {

  // 상세 페이지 단건 좋아요 여부
  boolean existsByAnimal_IdAndMember_Id(Long animalId, Long memberId);

  // 목록 페이지 배치 조회 - 이번 페이지의 animalId 중 내가 좋아요한 것만 (N+1 방지, 한 방 쿼리)
  @Query("select al.animal.id from AnimalLike al "
    + "where al.member.id = :memberId and al.animal.id in :animalIds")
  List<Long> findLikedAnimalIds(@Param("memberId") Long memberId,
                                @Param("animalIds") List<Long> animalIds);

  // "좋아요만" 필터용 - 이 회원이 좋아요한 전체 animalId (RDB 커밋 기준).
  // 여기에 Redis flush 안된 것을 오버레이해 "현재 시점" 집합을 만든다.
  @Query("select al.animal.id from AnimalLike al where al.member.id = :memberId")
  List<Long> findAllLikedAnimalIds(@Param("memberId") Long memberId);

  // 마이페이지 - 내가 좋아요한 동물 목록(최근 좋아요 순)
  @Query("select al.animal from AnimalLike al "
    + "where al.member.id = :memberId order by al.createdAt desc")
  Page<Animal> findLikedAnimals(@Param("memberId") Long memberId, Pageable pageable);


}