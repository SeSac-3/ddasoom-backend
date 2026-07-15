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

    // 조회 수 테스트
    @Test
    @DisplayName("조회 수가 증가한다")
    void viewCountTest() {

        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        // when
        post.increaseViewCount();
        post.increaseViewCount();
        post.increaseViewCount();

        // then
        assertThat(post.getViewCount()).isEqualTo(3);
    }

    // 댓글 수 테스트
    @Test
    @DisplayName("댓글 수가 정상적으로 오르고 내린다")
    void commentCountTest() {

        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        // when
        post.increaseCommentCount();
        post.increaseCommentCount();
        post.decreaseCommentCount();

        // then
        assertThat(post.getCommentCount()).isEqualTo(1);
    }

    // 게시글 업데이트 테스트
    @Test
    @DisplayName("게시글을 수정하면 게시판·카테고리·제목·본문이 모두 변경된다")
    void updatePostTest() {

        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .boardType(BoardType.CAT_INFO)
                .category("예방접종")
                .build();

        // when
        post.update(BoardType.DOG_INFO, "사료", "수정된 제목", "수정된 내용");

        // then
        assertThat(post.getBoardType()).isEqualTo(BoardType.DOG_INFO);
        assertThat(post.getCategory()).isEqualTo("사료");
        assertThat(post.getTitle()).isEqualTo("수정된 제목");
        assertThat(post.getContent()).isEqualTo("수정된 내용");
    }

    // soft-delete 테스트
    @Test
    @DisplayName("게시글을 삭제하면 isDeleted 상태가 true로 바뀌고, 삭제 시간이 기록된다.")
    void softDeleteTest() {

        // given
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .build();

        // when
        post.softDelete();

        // then
        assertThat(post.isDeleted()).isTrue();
        assertThat(post.getDeletedAt()).isNotNull();
    }
}