package com.paw.ddasoom.support.domain;

import java.time.LocalDateTime;

import com.paw.ddasoom.common.util.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "faq")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Faq extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "faq_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FaqCategory category;

    @Column(nullable = false, length = 255)
    private String question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String answer;

    @Column(nullable = false)
    private Boolean isVisible=true;

    @Column(columnDefinition = "DATETIME(6)")
    private LocalDateTime deletedAt;

    @Builder
    public Faq(FaqCategory category, String question, String answer) {
        this.category = category;
        this.question = question;
        this.answer = answer;
    }

    /* [FAQ 내용 수정] */
    public void update(FaqCategory category, String question, String answer) {
        this.category = category;
        this.question = question;
        this.answer = answer;
    }

    /* [FAQ 노출 여부 변경] */
    public void changeVisibility(boolean isVisible) {
        this.isVisible = isVisible;
    }

    /* [FAQ 논리 삭제 처리] */
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }

    /* [FAQ 삭제 상태 확인] */
    public boolean isDeleted() {
        return this.deletedAt != null;
    }
}

