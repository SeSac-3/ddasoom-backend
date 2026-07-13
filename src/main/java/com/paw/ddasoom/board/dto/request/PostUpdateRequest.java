package com.paw.ddasoom.board.dto.request;

import com.paw.ddasoom.board.domain.BoardType;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostUpdateRequest {

    private BoardType boardType;
    private String category;
    private String title;
    private String content;

    /** 수정 후 최종 이미지 ID 목록 — 기존 활성 중 여기 없는 것은 soft delete (syncImages diff 기준) */
    private List<Long> imageIds;

    /** 대표 이미지 재지정 — 미지정 시 null (기존 썸네일이 imageIds에서 빠지면 프론트가 재지정 유도) */
    private Long thumbnailImageId;
}