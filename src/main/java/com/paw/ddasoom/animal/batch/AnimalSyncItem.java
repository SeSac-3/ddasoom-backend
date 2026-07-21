package com.paw.ddasoom.animal.batch;

import java.time.LocalDateTime;

import com.paw.ddasoom.animal.domain.AnimalGender;
import com.paw.ddasoom.animal.domain.AnimalKind;

/**
 * animal 벌크 upsert(JDBC) 전용 아이템.
 * AnimalSyncService에서 공공API 원본(AnimalFetchResponse.AnimalItem)을 검증/변환까지 끝낸 후 생성한다.
 * (enum 변환 실패, 날짜 파싱 실패 등은 이 레코드가 만들어지기 전에 걸러진다 - AnimalSyncService.toSyncItem 참고)
 *
 * nickname은 "신규 INSERT일 때만" 반영된다 - AnimalSyncJdbcRepository의 SQL이
 * ON DUPLICATE KEY UPDATE 절에 nickname을 포함하지 않기 때문에,
 * 기존 행이면 DB에 이미 있던 닉네임이 그대로 유지된다(덮어쓰지 않음).
 */
public record AnimalSyncItem(
  String abandonmentId,
  AnimalKind kind,
  String nickname,
  AnimalGender gender,
  String typeName,
  String age,
  String location,
  String weight,
  String color,
  String specialMark,
  String vaccinationChk,
  String imageUrl,
  LocalDateTime rescuedAt
) {}