package com.paw.ddasoom.board.repository;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.dto.projection.PostListDto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 게시글 목록 조회 — 카드 UI 전용 projection.
     * content 전체 대신 SUBSTRING으로 미리보기만 가져오고(무거운 컬럼 회피),
     * member는 JOIN으로 닉네임만 평면화(N+1 방지).
     */
    @Query("""
        SELECT NEW com.paw.ddasoom.board.dto.projection.PostListDto(
            p.id, p.category, p.title, SUBSTRING(p.content, 1, 200),
            p.viewCount, p.commentCount, p.createdAt,
            m.id, m.nickname)
        FROM Post p
        JOIN p.member m
        WHERE p.boardType = :boardType
          AND p.category = :category
          AND p.deletedAt IS NULL
        ORDER BY p.createdAt DESC
        """)
    Page<PostListDto> findPostList(
            @Param("boardType") BoardType boardType,
            @Param("category") String category,
            Pageable pageable);

    /**
     * 게시글 상세 조회 — member fetch join.
     * 단건 조회라 컬렉션 fetch join 페이징 이슈 없음.
     */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.member
        WHERE p.id = :postId
          AND p.deletedAt IS NULL
        """)
    Optional<Post> findDetailById(@Param("postId") Long postId);
}