package com.paw.ddasoom.board.repository;

import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.dto.projection.CommentListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    /** 수정/삭제용 — 존재 + 활성 + 소속(postId) 세 검증을 쿼리 하나로 */
    Optional<PostComment> findByIdAndPostIdAndDeletedAtIsNull(Long commentId, Long postId);

    /**
     * 목록 조회용 Projection.
     * ⚠️ SELECT NEW 뒤 경로는 반드시 FQCN(패키지 전체 경로)이어야 함.
     *    IDE rename 리팩터링이 이 문자열을 갱신하지 않으므로, 클래스 이동/개명 시 수동 수정 필수.
     */
    @Query(
            value = "SELECT new com.paw.ddasoom.board.dto.projection.CommentListProjection(" +
                    "c.id, m.id, m.nickname, c.content, c.createdAt, c.updatedAt) " +
                    "FROM PostComment c JOIN c.member m " +
                    "WHERE c.post.id = :postId AND c.deletedAt IS NULL " +
                    "ORDER BY c.createdAt ASC",
            countQuery = "SELECT count(c) FROM PostComment c " +
                    "WHERE c.post.id = :postId AND c.deletedAt IS NULL"
    )
    Page<CommentListProjection> findCommentsByPostId(@Param("postId") Long postId, Pageable pageable);
}