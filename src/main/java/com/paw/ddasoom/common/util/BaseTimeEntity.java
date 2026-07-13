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

  @Generated(event = EventType.INSERT)
  @Column(insertable = false, updatable = false, nullable = false, columnDefinition = "DATETIME(6)")
  private LocalDateTime createdAt;

  @Generated(event = { EventType.INSERT, EventType.UPDATE })
  @Column(insertable = false, updatable = false, nullable = false, columnDefinition = "DATETIME(6)")
  private LocalDateTime updatedAt;
}