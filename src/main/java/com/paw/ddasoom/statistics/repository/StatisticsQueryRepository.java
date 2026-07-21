package com.paw.ddasoom.statistics.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import com.paw.ddasoom.animal.domain.QAnimal;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.domain.QFoster;
import com.paw.ddasoom.qna.domain.QQna;
import com.paw.ddasoom.qna.domain.QnaStatus;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * 대시보드·통계 전용 집계 쿼리의 단일 창구.
 * 타 도메인(foster/qna/animal) Repository에 집계 메서드를 흩뿌리지 않기 위해 한 곳에 모은다 — 팀 결정.
 * 읽기 전용 집계만 담당하며, 타 도메인 엔티티에 대한 쓰기는 절대 하지 않는다 (단방향 읽기 의존만 허용).
 * dashboard 도메인도 이 저장소를 공유한다 (두 도메인 모두 집계 소비자 — 담당: 지훈).
 */
@Repository
@RequiredArgsConstructor
public class StatisticsQueryRepository {

  private final JPAQueryFactory queryFactory;
  // 지역 분포의 SUBSTRING_INDEX(MySQL 전용 함수)는 JPQL이 몰라 네이티브 쿼리 필요 — 이 1건만 EntityManager 직접 사용
    private final EntityManager em;

    /** 임보 신청 상태별 현재 분포 — 심사대기 카운트와 상태 분포 차트가 공유하는 단일 쿼리 */
    public Map<FosterStatus, Long> countFostersByStatus() {
        QFoster foster = QFoster.foster;
        return queryFactory
                .select(foster.status, foster.count())
                .from(foster)
                .where(foster.deletedAt.isNull())   // 활성 신청만 (soft delete 제외)
                .groupBy(foster.status)
                .fetch()
                .stream()
                .collect(Collectors.toMap(
                        tuple -> tuple.get(foster.status),
                        tuple -> Optional.ofNullable(tuple.get(foster.count())).orElse(0L)));
    }

    /**
     * 만료 임박 임보 건수 — [from, toExclusive) 반개구간이라 호출부가 구간을 이어 붙여도 이중 카운트가 없다.
     * 진행 중(FOSTERING/EXTENDED) 신청만 대상 — 종료/거절/대기 건은 "만료"라는 개념이 없음.
     */
    public long countExpiringFosters(LocalDateTime from, LocalDateTime toExclusive) {
        QFoster foster = QFoster.foster;
        Long count = queryFactory
                .select(foster.count())
                .from(foster)
                .where(foster.deletedAt.isNull(),
                        foster.status.in(FosterStatus.FOSTERING, FosterStatus.EXTENDED),
                        foster.fosterEndAt.goe(from),
                        foster.fosterEndAt.lt(toExclusive))
                .fetchOne();
        return count != null ? count : 0L;
    }

    /** 답변 대기 QnA 건수 (Qna는 soft delete 미적용 도메인이라 deletedAt 조건 없음) */
    public long countPendingQnas() {
        QQna qna = QQna.qna;
        Long count = queryFactory
                .select(qna.count())
                .from(qna)
                .where(qna.status.eq(QnaStatus.PENDING))
                .fetchOne();
        return count != null ? count : 0L;
    }

  /** 특정 연도의 월별 임보 신청 건수 — [1월 1일, 익년 1월 1일) 반개구간, month로 group by */
    public List<Tuple> countMonthlyFosters(LocalDateTime yearStart, LocalDateTime nextYearStart) {
        QFoster foster = QFoster.foster;
        return queryFactory
                .select(foster.createdAt.month(), foster.count())
                .from(foster)
                .where(foster.deletedAt.isNull(),
                        foster.createdAt.goe(yearStart),
                        foster.createdAt.lt(nextYearStart))
                .groupBy(foster.createdAt.month())
                .fetch();
    }

    /**
     * 평균 임보 지속기간 계산용 (시작/종료 시각 쌍) — 일수 차이 계산은 서비스에서 수행.
     * DB의 TIMESTAMPDIFF는 JPQL 표준이 아니라 이식성 문제가 있고, 대상이 수백 건 규모라
     * 애플리케이션 계산이 안전·충분하다는 판단.
     * 대상: 기간이 성립한 신청만 — 진행중/연장/종료 (대기·거절은 기간 개념이 없음)
     */
    public List<Tuple> findFosterDurations() {
        QFoster foster = QFoster.foster;
        return queryFactory
                .select(foster.fosterStartAt, foster.fosterEndAt)
                .from(foster)
                .where(foster.deletedAt.isNull(),
                        foster.status.in(FosterStatus.FOSTERING, FosterStatus.EXTENDED, FosterStatus.ENDED),
                        foster.fosterStartAt.isNotNull(),
                        foster.fosterEndAt.isNotNull())
                .fetch();
    }

    /** 종별(개/고양이) 등록 동물 수 */
    public List<Tuple> countAnimalsByKind() {
        QAnimal animal = QAnimal.animal;
        return queryFactory
                .select(animal.kind, animal.count())
                .from(animal)
                .groupBy(animal.kind)
                .fetch();
    }

    /**
     * 보호지역 시/도별 등록 동물 분포 — location("경기도 안산시 ...")의 첫 공백 전 토큰을 시/도로 집계.
     * SUBSTRING_INDEX는 MySQL 전용이라 네이티브 쿼리 사용 (프로젝트가 MySQL 고정 — Flyway SQL과 동일 전제).
     * 반환: [0]=시/도 문자열, [1]=건수 — 건수 내림차순
     */
    @SuppressWarnings("unchecked")
    public List<Object[]> countAnimalsByRegion() {
        return em.createNativeQuery("""
                SELECT SUBSTRING_INDEX(location, ' ', 1) AS sido, COUNT(*) AS cnt
                FROM animal
                GROUP BY sido
                ORDER BY cnt DESC
                """).getResultList();
    }

    /**
     * 임보 신청 많은 동물 TOP10 — 신청 건수(전 상태) 기준. 동물 정보는 조인으로 함께.
     * groupBy에 동물의 표시 필드를 전부 넣는 이유: SELECT에 집계 아닌 컬럼이 오면 group by에 포함돼야 하고
     * (MySQL only_full_group_by), 이 필드들은 animal.id에 함수 종속이라 결과 행 수에 영향을 주지 않는다.
     * 종별 필터는 여기서 하지 않고 프론트가 kind로 거른다(전체 순위를 유지한 채 필터하기 위함).
     */
    public List<Tuple> findTopFosterAnimals(int limit) {
        QFoster foster = QFoster.foster;
        QAnimal animal = QAnimal.animal;
        return queryFactory
                .select(animal.id, animal.nickname, animal.abandonmentId,
                        animal.kind, animal.typeName, animal.imageUrl, foster.count())
                .from(foster)
                .join(foster.animal, animal)
                .where(foster.deletedAt.isNull())
                .groupBy(animal.id, animal.nickname, animal.abandonmentId,
                        animal.kind, animal.typeName, animal.imageUrl)
                .orderBy(foster.count().desc())
                .limit(limit)
                .fetch();
    }
}
