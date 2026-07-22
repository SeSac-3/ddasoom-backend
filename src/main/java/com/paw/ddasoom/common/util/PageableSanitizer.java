package com.paw.ddasoom.common.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import lombok.extern.slf4j.Slf4j;

/**
 * 클라이언트가 보낸 페이징·정렬 파라미터를 안전한 값으로 정규화하는 공통 유틸.
 *
 * <p><b>왜 필요한가</b> — {@code @PageableDefault Pageable}을 그대로 리포지토리에 넘기면
 * 클라이언트가 {@code ?sort=member.password,desc} 같은 임의 프로퍼티 경로를 주입할 수 있다.
 * 조인 여부에 따라 ① 정렬 순서로 비공개 값을 추론하거나 ② QuerySyntaxException으로 500이 뜬다.
 * {@code ?size=100000}이면 전체 테이블이 한 번에 끌려나온다.
 * {@code @PageableDefault(sort=...)}는 <b>방어가 아니다</b> — 클라이언트 값이 기본값을 덮어쓴다.
 *
 * <p><b>정책</b> — 정렬은 화이트리스트 대조 후 미허용 프로퍼티를 버리고(전부 버려지면 기본 정렬로 대체),
 * size는 상한으로 클램프한다. 조용히 기본값으로 떨어지면 디버깅이 어려우므로 버려진 프로퍼티는 debug 로그를 남긴다.
 *
 * <p>⚠️ <b>적용 대상은 "클라이언트 요청" 페이징뿐이다.</b> 서버가 외부 공공 API를 순회할 때 쓰는
 * 페이지 크기(예: AnimalFetchService의 numOfRows=1000, 8천 건 이상 적재)는 사용자 입력이 아니므로
 * 이 유틸을 적용하지 않는다 — 적용하면 동기화가 50건에서 잘린다.
 */
@Slf4j
public final class PageableSanitizer {

  /** 목록 조회 1회 최대 건수. 화면 최대 노출(9~20)의 여유분이자 대량 조회 차단선 */
    public static final int MAX_SIZE = 50;
    private static final int MIN_SIZE = 1;

    private PageableSanitizer() {   // 유틸 클래스 — 인스턴스화 금지
    }

    /**
     * {@code @PageableDefault Pageable}용 — 정렬 화이트리스트 + size 클램프.
     *
     * @param pageable         클라이언트가 보낸 원본
     * @param defaultSort      허용된 정렬이 하나도 없을 때 대체할 기본 정렬
     * @param allowedSortProps 허용 프로퍼티(엔티티 필드명). 비우면 정렬을 전부 기본값으로 강제
     */
    public static Pageable sanitize(Pageable pageable, Sort defaultSort, String... allowedSortProps) {
        Set<String> allowed = Arrays.stream(allowedSortProps).collect(Collectors.toSet());

        List<Sort.Order> kept = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            if (allowed.contains(order.getProperty())) {
                kept.add(order);
            } else {
                // 조용히 사라지면 "정렬이 왜 안 먹지?"로 헤매게 되므로 흔적을 남긴다
                log.debug("허용되지 않은 정렬 프로퍼티 무시 - property: {}", order.getProperty());
            }
        }

        Sort safeSort = kept.isEmpty() ? defaultSort : Sort.by(kept);
        return PageRequest.of(clampPage(pageable.getPageNumber()), clampSize(pageable.getPageSize()), safeSort);
    }

    /**
     * {@code @RequestParam page/size}용 — {@code PageRequest.of(page, size)}의 안전한 대체.
     * 이 방식은 sort 바인딩 자체가 없어 정렬 주입은 불가능하고, size·page 범위만 방어하면 된다.
     * (page 음수/size 0은 PageRequest.of가 IllegalArgumentException을 던져 500이 되므로 함께 클램프)
     */
    public static Pageable of(int page, int size) {
        return PageRequest.of(clampPage(page), clampSize(size));
    }

    private static int clampSize(int size) {
        return Math.min(Math.max(size, MIN_SIZE), MAX_SIZE);
    }

    private static int clampPage(int page) {
        return Math.max(page, 0);
    }

}
