package com.paw.ddasoom.animal.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;

@Component
public class AnimalDateConverter {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public LocalDateTime convert(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(rawDate, FORMATTER);
            return date.atStartOfDay(); // 00:00:00으로 세팅
        } catch (Exception e) {
            throw new IllegalArgumentException("날짜 형식을 파싱할 수 없습니다: " + rawDate, e);
        }
    }
}