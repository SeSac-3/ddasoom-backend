package com.paw.ddasoom.board.dto.request;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.member.domain.Member;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    private String boardType;
    private String category;
    private String title;
    private String content;

    /** 하이브리드 업로드 확정 연결용 — 작성 중 업로드된 이미지 ID 목록 (본문 삽입 순서 = 리스트 순서) */
    private List<Long> imageIds;

    /** 대표 이미지 명시 지정 (사용자 직접 선택) — 미지정 시 null */
    private Long thumbnailImageId;

    public Post toEntity(Member member, BoardType boardType) {
        return Post.builder()
                .member(member)
                .boardType(boardType)
                .category(category)
                .title(title)
                .content(content)
                .build();
    }
}