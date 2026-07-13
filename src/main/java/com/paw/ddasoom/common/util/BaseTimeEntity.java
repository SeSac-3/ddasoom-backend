package com.paw.ddasoom.common.util;

import java.time.LocalDateTime;

import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

@Getter
@MappedSuperclass
public abstract class BaseTimeEntity {

  // 시간의 주인 = DB (DEFAULT / ON UPDATE CURRENT_TIMESTAMP(6)) — 앱은 읽기만 한다. (V6 마이그레이션과 세트)
  // @Generated: INSERT/UPDATE 직후 Hibernate가 DB가 채운 값을 다시 읽어 엔티티에 반영
  //             (save() 직후 응답 DTO에 createdAt을 쓰는 기존 코드가 깨지지 않게 하는 핵심)
  // insertable/updatable = false: 앱이 이 컬럼에 값을 쓰지 않음을 매핑 수준에서 강제
  @Generated(event = EventType.INSERT)
  @Column(insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
  private LocalDateTime createdAt;

  @Generated(event = { EventType.INSERT, EventType.UPDATE })
  @Column(insertable = false, updatable = false, columnDefinition = "DATETIME(6)")
  private LocalDateTime updatedAt;
}
