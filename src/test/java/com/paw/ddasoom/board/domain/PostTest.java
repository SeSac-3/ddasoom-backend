package com.paw.ddasoom.board.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PostTest {

    // 댓글 수 음수 방지 테스트
    @Test
    @DisplayName("댓글 감소가 0에서 멈춘다.")
    void decreaseCommentTest() {

        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        // when
        post.decreaseCommentCount();

        // then
        assertThat(post.getCommentCount()).isEqualTo(0);
    }

}
