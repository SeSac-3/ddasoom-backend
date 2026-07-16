package com.paw.ddasoom.board.dto.request;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.member.domain.Member;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {


    private String boardType;

    private String category;

    @NotBlank(message = "제목을 입력해주세요.")
    @Size(max = 255, message = "제목은 255자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용을 입력해주세요.")
    @Size(max = 10000, message = "본문이 제한된 길이를 초과했습니다.")
    private String content;

    /** 하이브리드 업로드 확정 연결용 — 작성 중 업로드된 이미지 ID 목록 (본문 삽입 순서 = 리스트 순서) */
    @Size(max = 20, message = "이미지는 최대 20장입니다.")
    private List<Long> imageIds;

    /** 대표 이미지 명시 지정 (사용자 직접 선택) — 미지정 시 null */
    private Long thumbnailImageId;

    // content 를 파라미터로 받는다 — Service에서 sanitize한 본문을 주입한다.
    public Post toEntity(Member member, BoardType boardType, String content) {
        return Post.builder()
                .member(member)
                .boardType(boardType)
                .category(category)
                .title(title)
                .content(content)   // 정제된 본문
                .build();
    }
}