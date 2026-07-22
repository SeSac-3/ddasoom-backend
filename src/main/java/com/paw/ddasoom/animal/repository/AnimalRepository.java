package com.paw.ddasoom.animal.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.paw.ddasoom.animal.domain.Animal;

import jakarta.persistence.LockModeType;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
  // API UPSERT
  Optional<Animal> findByAbandonmentId(String abandonmentId);

  // 메인 미리보기 — 최근 등록(PK 내림차순) 4건
  List<Animal> findTop4ByOrderByIdDesc();

  //같은 동물에 대한 관리자 동시 승인 요청을 직렬화(Foster)
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select a from Animal a where a.id = :animalId")
  Optional<Animal> findByIdForUpdate(@Param("animalId") Long animalId);
}
