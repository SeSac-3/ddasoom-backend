package com.paw.ddasoom.board.dto.request;

import com.paw.ddasoom.board.domain.BoardType;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PostCreateRequest {

    private BoardType boardType;
    private String category;
    private String title;
    private String content;
}
