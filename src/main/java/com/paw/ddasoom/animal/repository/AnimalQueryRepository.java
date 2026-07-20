package com.paw.ddasoom.animal.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;
import com.paw.ddasoom.animal.domain.QAnimal;
import com.paw.ddasoom.animal.dto.request.AnimalListPageRequest;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;

import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class AnimalQueryRepository {

  private final JPAQueryFactory queryFactory;

  /**
   * @param likedFilterIds "좋아요만" 필터로 제한할 animalId 집합.
   *        null = 필터 미적용(전체). 비어있지 않은 Set = 이 id들로만 제한.
   *        (좋아요 집합은 RDB 커밋 + Redis flush 안된 것들을 서비스에서 합쳐 넘겨준다 - read-your-writes)
   */
  private static final QAnimal animal = QAnimal.animal;

  public Page<Animal> search(AnimalListPageRequest request, Set<Long> likedFilterIds, Pageable pageable) {
    // memberId: "내가 좋아요한 동물만" 필터(isLiked=true)를 적용하기 위한 회원 PK. 비로그인이면 null.
    
    // WHERE 조건 — null 반환 메서드는 where절에서 자동 제거됨
    BooleanExpression[] conditions = {
      kindEq(request.kind()),
      locationEq(request.location()),
      isFosteredEq(request.isFostered()),
      genderEq(request.gender()),
      likedIn(likedFilterIds)
      // isLikedEq(request.isLiked(), memberId)
    };

    List<Animal> animals = queryFactory
      .selectFrom(animal)
      .where(conditions)
      .orderBy(animal.likeCount.desc(), animal.id.desc())
      .offset(pageable.getOffset())
      .limit(pageable.getPageSize())
      .fetch();

    // count 쿼리는 정렬이 의미 없으므로 orderBy 제거
    Long total = queryFactory
      .select(animal.count())
      .from(animal)
      .where(conditions)
      .fetchOne();

    return new PageImpl<>(animals, pageable, total == null ? 0 : total);
  }

  // 조건 메서드 — null 반환 시 where절에서 자동 제거
  private BooleanExpression kindEq(AnimalKind kind) {
    return kind != null ? animal.kind.eq(kind) : null;
  }

  private BooleanExpression locationEq(String location) {
    return location != null ? animal.location.contains(location) : null;
  }

  private BooleanExpression isFosteredEq(Boolean isFostered) {
    return isFostered != null ? animal.isFostered.eq(isFostered) : null;
  }

  private BooleanExpression genderEq(AnimalGender gender) {
    return gender != null ? animal.gender.eq(gender) : null;
  }

  // "좋아요만" 필터 - 서비스가 계산한 "현재 시점" 좋아요 집합으로 제한.
  // null(필터 미적용) 이면 조건 없음. (빈 집합은 서비스에서 미리 걸러 여기로 오지 않음)
  private BooleanExpression likedIn(Set<Long> likedFilterIds) {
    return likedFilterIds != null ? animal.id.in(likedFilterIds) : null;
  }
}
