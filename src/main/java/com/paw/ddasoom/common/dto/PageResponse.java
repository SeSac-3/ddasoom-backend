package com.paw.ddasoom.common.dto;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

import lombok.Builder;
import lombok.Getter;

/** 페이징 공통 응답 — Spring Page 직접 직렬화 금지(구조 불안정), 전 도메인 재사용 */
@Getter
@Builder
public class PageResponse<T> {
  private List<T> content;
  private int page;
  private int size;
  private long totalElements;
  private int totalPages;
  private boolean hasNext;
  private boolean hasPrevious;

  public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
    return PageResponse.<T>builder()
            .content(page.getContent().stream().map(mapper).toList())
            .page(page.getNumber())
            .size(page.getSize())
            .totalElements(page.getTotalElements())
            .totalPages(page.getTotalPages())
            .hasNext(page.hasNext())
            .hasPrevious(page.hasPrevious())
            .build();
  }

}