package com.paw.ddasoom.support.domain;

public enum FaqCategory {
    ACCOUNT("회원/계정"),
    FOSTER("임시보호"),
    ANIMAL_INFO("유기동물 정보"),
    COMMUNITY("커뮤니티"),
    SERVICE("서비스 이용"),
    ETC("기타");

    private final String label;

    FaqCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}