package com.paw.ddasoom.board.service;

import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.dto.request.CommentCreateRequest;
import com.paw.ddasoom.board.dto.request.CommentUpdateRequest;
import com.paw.ddasoom.board.dto.response.CommentResponse;
import com.paw.ddasoom.board.dto.projection.CommentListProjection;
import com.paw.ddasoom.board.dto.projection.MyCommentListProjection;
import com.paw.ddasoom.board.dto.response.MyCommentResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.board.exception.BoardErrorCode;
import com.paw.ddasoom.board.exception.BoardException;
import com.paw.ddasoom.board.repository.PostCommentRepository;
import com.paw.ddasoom.board.repository.PostRepository;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.member.service.MemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글 댓글(post_comment) 도메인 서비스.
 *
 * <p>댓글 CRUD와 목록 조회를 담당한다. 트랜잭션 경계는 이 서비스 계층에만 둔다(CONVENTIONS 6장).
 *
 * <p>설계 원칙 요약:
 * <ul>
 *   <li>{@code comment_count}는 캐시 컬럼이라, 댓글 생성/삭제 시 {@link Post}의 카운트를 함께 갱신한다.</li>
 *   <li>댓글 조회/수정/삭제는 항상 {@code (commentId, postId)} 쌍으로 활성 댓글을 찾아, 다른 글의
 *       댓글이 잘못 조작되지 않게 한다.</li>
 *   <li>삭제는 물리 삭제가 아닌 {@code softDelete}이며, 수정/삭제는 작성자 본인만 가능하다.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final MemberService memberService;

    /**
     * 댓글을 생성하고, 원글의 댓글 수 캐시({@code comment_count})를 1 증가시킨다.
     *
     * @param postId   댓글이 달릴 게시글 PK
     * @param memberId 작성자 회원 PK (토큰에서 추출된 값)
     * @param request  댓글 생성 요청 (본문)
     * @return 생성된 댓글 응답
     * @throws BoardException 게시글이 없거나 삭제되었을 때(POST_NOT_FOUND)
     * @throws MemberException 회원이 존재하지 않을 때(MEMBER_001)
     */
    @Transactional
    public CommentResponse create(Long postId, Long memberId, CommentCreateRequest request) {
        Post post = getActivePost(postId);
        Member member = memberService.getMember(memberId);

        PostComment comment = PostComment.builder()
                .post(post)
                .member(member)
                .content(request.getContent())
                .build();
        postCommentRepository.save(comment);

        post.increaseCommentCount();

        return CommentResponse.from(comment);
    }

    /**
     * 특정 게시글의 댓글 목록을 조회한다(오래된 순 페이징).
     *
     * <p>조회 전 원글 존재를 먼저 확인해, 없는 글의 댓글을 조회하려 하면 POST_NOT_FOUND로 응답한다.
     *
     * @param postId 대상 게시글 PK
     * @param page   0부터 시작하는 페이지 번호
     * @param size   페이지 크기
     * @return 댓글 목록 페이지
     * @throws BoardException 게시글이 없거나 삭제되었을 때(POST_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, int page, int size) {
        getActivePost(postId);   // 존재 확인 (없으면 POST_NOT_FOUND)

        Pageable pageable = PageRequest.of(page, size);
        Page<CommentListProjection> result =
                postCommentRepository.findCommentsByPostId(postId, pageable);

        return PageResponse.of(result, CommentResponse::from);
    }

    /**
     * 마이페이지 "내가 쓴 댓글" 목록을 조회한다(최신순 페이징).
     *
     * <p>원글이 soft delete된 댓글은 쿼리에서 제외된다 — 클릭 시 404가 되는 행을 노출하지 않기 위함
     * (repository {@code findMyComments} 주석 참고).
     *
     * @param memberId 조회 대상 회원 PK (토큰에서 추출된 본인)
     * @param pageable 페이지 정보
     * @return 본인 댓글 목록 페이지 (각 항목에 원글 제목·boardType 포함)
     */
    @Transactional(readOnly = true)
    public PageResponse<MyCommentResponse> getMyComments(Long memberId, Pageable pageable) {
        Page<MyCommentListProjection> result =
                postCommentRepository.findMyComments(memberId, pageable);

        return PageResponse.of(result, MyCommentResponse::from);
    }

    /**
     * 댓글 본문을 수정한다. 작성자 본인만 수정할 수 있다.
     *
     * @param postId    댓글이 속한 게시글 PK
     * @param commentId 수정 대상 댓글 PK
     * @param memberId  요청자 회원 PK (작성자 검증용)
     * @param request   댓글 수정 요청 (본문)
     * @return 수정된 댓글 응답
     * @throws BoardException 댓글 없음(COMMENT_NOT_FOUND), 작성자 불일치(COMMENT_ACCESS_DENIED)
     */
    @Transactional
    public CommentResponse update(Long postId, Long commentId, Long memberId, CommentUpdateRequest request) {
        PostComment comment = getActiveComment(commentId, postId);
        validateAuthor(comment, memberId);

        comment.updateContent(request.getContent());

        return CommentResponse.from(comment);
    }

    /**
     * 댓글을 soft delete하고, 원글의 댓글 수 캐시({@code comment_count})를 1 감소시킨다.
     * 작성자 본인만 삭제할 수 있다.
     *
     * @param postId    댓글이 속한 게시글 PK
     * @param commentId 삭제 대상 댓글 PK
     * @param memberId  요청자 회원 PK (작성자 검증용)
     * @throws BoardException 댓글 없음(COMMENT_NOT_FOUND), 작성자 불일치(COMMENT_ACCESS_DENIED)
     */
    @Transactional
    public void delete(Long postId, Long commentId, Long memberId) {
        PostComment comment = getActiveComment(commentId, postId);
        validateAuthor(comment, memberId);

        comment.softDelete();

        Post post = comment.getPost();
        post.decreaseCommentCount();
    }

    // ===== private =====

    /**
     * 활성 게시글을 조회한다 — 없거나 삭제되었으면 예외.
     *
     * @param postId 게시글 PK
     * @return 게시글 엔티티
     * @throws BoardException 게시글이 없거나 삭제되었을 때(POST_NOT_FOUND)
     */
    private Post getActivePost(Long postId) {
        return postRepository.findDetailById(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }


    /**
     * 특정 게시글에 속한 활성 댓글을 조회한다 — 없거나 삭제되었으면 예외.
     *
     * <p>{@code (commentId, postId)} 쌍으로 찾아, 다른 게시글의 댓글이 잘못 조작되는 것을 방지한다.
     *
     * @param commentId 댓글 PK
     * @param postId    댓글이 속해야 하는 게시글 PK
     * @return 댓글 엔티티
     * @throws BoardException 해당 게시글의 활성 댓글이 없을 때(COMMENT_NOT_FOUND)
     */
    private PostComment getActiveComment(Long commentId, Long postId) {
        return postCommentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
    }

    /**
     * 댓글 작성자 본인인지 검증한다.
     *
     * <p>⚠️ {@code Long}은 {@code ==} 비교 금지(캐시 범위 밖에서는 항상 false) — {@code equals} 필수.
     *
     * @param comment  대상 댓글
     * @param memberId 요청자 회원 PK
     * @throws BoardException 작성자가 아닐 때(COMMENT_ACCESS_DENIED)
     */
    private void validateAuthor(PostComment comment, Long memberId) {
        if (!comment.getMember().getId().equals(memberId)) {
            throw new BoardException(BoardErrorCode.COMMENT_ACCESS_DENIED);
        }
    }
}