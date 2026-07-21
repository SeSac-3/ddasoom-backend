package com.paw.ddasoom.board.repository;

import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.dto.projection.CommentListProjection;
import com.paw.ddasoom.board.dto.projection.MyCommentListProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 게시글 댓글(post_comment) 레포지토리.
 *
 * <p>목록 조회는 projection({@code SELECT NEW})으로 필요한 컬럼만 평면화하며,
 * 수정/삭제용 단건 조회는 {@code (commentId, postId)} 쌍 + 활성 조건을 쿼리 하나로 검증한다.
 *
 * <p>⚠️ {@code SELECT NEW} 뒤의 경로는 반드시 FQCN(패키지 전체 경로) 문자열이어야 한다 —
 * IDE rename 리팩터링이 이 문자열을 갱신하지 않으므로, projection 클래스를 이동/개명하면 수동으로 고쳐야 한다.
 */
public interface PostCommentRepository extends JpaRepository<PostComment, Long> {

    /**
     * 수정/삭제용 단건 조회 — 존재 + 활성 + 소속(postId) 세 검증을 쿼리 하나로.
     *
     * <p>{@code postId}까지 조건에 넣어, 다른 게시글의 댓글이 잘못 조작되는 것을 막는다.
     *
     * @param commentId 댓글 PK
     * @param postId    댓글이 속해야 하는 게시글 PK
     * @return 활성 댓글, 없으면 {@code Optional.empty()}
     */
    Optional<PostComment> findByIdAndPostIdAndDeletedAtIsNull(Long commentId, Long postId);

    /**
     * 특정 게시글의 댓글 목록 조회용 projection — member 조인으로 작성자 닉네임 평면화(N+1 방지).
     * 활성 댓글만, 오래된 순 정렬.
     *
     * @param postId   대상 게시글 PK
     * @param pageable 페이지 정보
     * @return 댓글 목록 projection 페이지
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

    /**
     * 마이페이지 "내가 쓴 댓글" 목록 — post 조인으로 원글 제목/보드타입 평면화(행 클릭 이동용).
     *
     * <p>원글이 soft delete된 댓글은 제외한다({@code p.deletedAt IS NULL}) — 클릭 시 404가 되는 행을
     * 노출하지 않기 위함. 본인 활성 댓글만, 최신순 정렬.
     *
     * @param memberId 조회 대상 회원 PK (본인)
     * @param pageable 페이지 정보
     * @return 내가 쓴 댓글 목록 projection 페이지
     */
    @Query(
            value = "SELECT new com.paw.ddasoom.board.dto.projection.MyCommentListProjection(" +
                    "c.id, c.content, c.createdAt, p.id, p.title, p.boardType) " +
                    "FROM PostComment c JOIN c.post p " +
                    "WHERE c.member.id = :memberId AND c.deletedAt IS NULL AND p.deletedAt IS NULL " +
                    "ORDER BY c.createdAt DESC",
            countQuery = "SELECT count(c) FROM PostComment c JOIN c.post p " +
                    "WHERE c.member.id = :memberId AND c.deletedAt IS NULL AND p.deletedAt IS NULL"
    )
    Page<MyCommentListProjection> findMyComments(@Param("memberId") Long memberId, Pageable pageable);
}