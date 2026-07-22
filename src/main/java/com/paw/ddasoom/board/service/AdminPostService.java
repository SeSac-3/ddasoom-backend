package com.paw.ddasoom.board.service;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.dto.projection.AdminAllCommentListProjection;
import com.paw.ddasoom.board.dto.projection.AdminCommentListProjection;
import com.paw.ddasoom.board.dto.projection.AdminPostListProjection;
import com.paw.ddasoom.board.dto.response.AdminAllCommentResponse;
import com.paw.ddasoom.board.dto.response.AdminCommentResponse;
import com.paw.ddasoom.board.dto.response.AdminPostDetailResponse;
import com.paw.ddasoom.board.dto.response.AdminPostResponse;
import com.paw.ddasoom.board.exception.BoardErrorCode;
import com.paw.ddasoom.board.exception.BoardException;
import com.paw.ddasoom.board.repository.PostCommentRepository;
import com.paw.ddasoom.board.repository.PostRepository;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 게시판(post / post_comment) 서비스.
 *
 * <p>관리자 전용 목록/상세 조회와 <b>강제삭제</b>를 담당한다. 사용자용 {@link PostService}/
 * {@link PostCommentService}와 컨트롤러·서비스를 분리한 이유:
 * <ul>
 *   <li>조회 대상이 다르다 — 관리자는 삭제된 글/댓글도 봐야 하므로 {@code deletedAt IS NULL} 필터가 없는
 *       별도 쿼리를 쓴다(AdminMemberService가 탈퇴 회원을 포함 조회하는 것과 동일 구조).</li>
 *   <li>권한 모델이 다르다 — 강제삭제는 <b>작성자 검증을 하지 않는다</b>(관리자는 타인 글도 삭제 가능).
 *       사용자 삭제 경로의 {@code validateAuthor}를 재사용하면 안 되는 이유.</li>
 * </ul>
 *
 * <p>설계 원칙:
 * <ul>
 *   <li>강제삭제도 물리 삭제가 아닌 {@code softDelete}다(DB 컨벤션 — 물리 DELETE 금지).</li>
 *   <li>강제삭제는 <b>멱등(idempotent)</b>이다 — 이미 삭제된 대상은 예외 없이 no-op으로 넘긴다.
 *       신고 승인(ReportService.hideTarget)이 같은 대상에 대해 여러 번 호출될 수 있고,
 *       작성자가 먼저 지운 글을 관리자가 다시 처리할 수 있기 때문. (AdminMemberService.hideMember의
 *       "이미 탈퇴면 no-op"과 동일 철학)</li>
 * </ul>
 *
 * <p>⚠️ {@link #forceDeletePost(Long)} / {@link #forceDeleteComment(Long)}는 신고 도메인이 재사용한다.
 * ReportService.hideTarget()의 POST / POST_COMMENT 분기가 이 두 메서드에 위임하도록 연결된다
 * (회원 제재를 adminMemberService.forceWithdraw에 위임하는 것과 대칭).
 */
@Service
@RequiredArgsConstructor
public class AdminPostService {

    private final PostRepository postRepository;
    private final PostCommentRepository postCommentRepository;
    private final ImageService imageService;

    // ===== 조회 =====

    /**
     * 관리자 게시글 목록을 조회한다(전 보드 통합, 삭제된 글 포함).
     *
     * <p>{@code boardType}/{@code keyword}는 선택 필터다 — null/빈 값이면 조건에서 빠진다.
     * 목록 필터는 생성/수정과 달리 엄격 검증 대상이 아니지만, boardType 값이 들어왔는데 미존재
     * enum이면 BOARD_003으로 규격 응답한다(사용자 목록 조회와 동일 정책).
     *
     * @param boardType 보드 타입 필터 (null/빈 값이면 전 보드)
     * @param keyword   제목 부분일치 검색어 (null/빈 값이면 검색 없음)
     * @param pageable  페이지 정보
     * @return 관리자 게시글 목록 페이지 (삭제 글 포함, 상태는 deletedAt으로 구분)
     * @throws BoardException boardType 값이 있는데 유효하지 않을 때(BOARD_003)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminPostResponse> getPosts(String boardType, String keyword, Pageable pageable) {
        BoardType parsedBoardType =
                boardType != null && !boardType.isBlank() ? parseBoardType(boardType) : null;
        String normalizedKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;

        Page<AdminPostListProjection> page =
                postRepository.findPostsForAdmin(parsedBoardType, normalizedKeyword, pageable);

        return PageResponse.of(page, AdminPostResponse::from);
    }

    /**
     * 관리자 게시글 상세를 조회한다 — 삭제된 글도 조회 가능, 조회수는 올리지 않는다.
     *
     * <p>이미지는 활성 이미지만 노출된다({@link ImageService#getImages}). 강제삭제된 글은 이미지도 함께
     * 정리(soft delete)되므로 이미지 목록이 비어 나올 수 있다.
     *
     * @param postId 게시글 PK
     * @return 관리자 게시글 상세 응답 (본문 + 활성 이미지 + 삭제 상태)
     * @throws BoardException 게시글이 존재하지 않을 때(POST_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public AdminPostDetailResponse getPostDetail(Long postId) {
        Post post = getPostIncludingDeleted(postId);
        List<ImageResponse> images = imageService.getImages(OwnerType.POST, postId);
        return AdminPostDetailResponse.from(post, images);
    }

    /**
     * 관리자용 특정 게시글의 댓글 목록을 조회한다(삭제된 댓글 포함).
     *
     * <p>조회 전 원글 존재를 먼저 확인한다(삭제된 글도 존재로 간주 — 관리자는 삭제된 글의 댓글도 열람 가능).
     *
     * @param postId   대상 게시글 PK
     * @param pageable 페이지 정보
     * @return 관리자 댓글 목록 페이지 (삭제 댓글 포함, 상태는 deletedAt으로 구분)
     * @throws BoardException 게시글이 존재하지 않을 때(POST_NOT_FOUND)
     */
    @Transactional(readOnly = true)
    public PageResponse<AdminCommentResponse> getComments(Long postId, Pageable pageable) {
        getPostIncludingDeleted(postId);   // 존재 확인 (없으면 POST_NOT_FOUND)

        Page<AdminCommentListProjection> page =
                postCommentRepository.findCommentsForAdmin(postId, pageable);

        return PageResponse.of(page, AdminCommentResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAllCommentResponse> getAllComments(Pageable pageable) {
        Page<AdminAllCommentListProjection> page =
                postCommentRepository.findAllCommentsForAdmin(pageable);
        return PageResponse.of(page, AdminAllCommentResponse::from);
    }

    // ===== 강제삭제 =====

    /**
     * 게시글을 강제 삭제(soft delete)한다 — 작성자 검증 없이 관리자 권한으로 수행.
     *
     * <p>이미 삭제된 글이면 아무 것도 하지 않는다(멱등). 소유자 삭제와 동일하게 연결된 이미지도
     * 빈 리스트 {@code syncImages}로 함께 정리한다(IMAGE_FLOW 3-6 패턴).
     *
     * <p>⚠️ 신고 도메인이 이 메서드를 재사용한다 — 시그니처/이름 변경 시 ReportService.hideTarget 확인 필요.
     *
     * @param postId 삭제 대상 게시글 PK
     * @throws BoardException 게시글이 존재하지 않을 때(POST_NOT_FOUND)
     */
    @Transactional
    public void forceDeletePost(Long postId) {
        Post post = getPostIncludingDeleted(postId);
        if (post.isDeleted()) {
            return;   // 멱등 — 이미 삭제된 글은 중복 처리하지 않음
        }

        post.softDelete();
        // 소유자 삭제 시 이미지 일괄 정리 = 빈 리스트 sync (IMAGE_FLOW 3-6 패턴)
        // requesterId는 null — 빈 리스트라 attach가 조기 return하므로 업로더 검증 경로를 타지 않는다.
        // (관리자/신고 경유 강제 삭제라 "요청자"가 업로더일 이유도 없다. ReportService도 이 메서드를 재사용)
        imageService.syncImages(List.of(), OwnerType.POST, postId, null);
    }

    /**
     * 댓글을 강제 삭제(soft delete)한다 — 작성자 검증 없이 관리자 권한으로 수행.
     *
     * <p>이미 삭제된 댓글이면 아무 것도 하지 않는다(멱등). 활성 댓글을 지울 때만 원글의
     * {@code comment_count} 캐시를 1 감소시킨다(이미 삭제분 재호출로 인한 이중 감소 방지).
     *
     * <p>⚠️ 신고 도메인이 이 메서드를 재사용한다 — 시그니처/이름 변경 시 ReportService.hideTarget 확인 필요.
     *
     * @param commentId 삭제 대상 댓글 PK
     * @throws BoardException 댓글이 존재하지 않을 때(COMMENT_NOT_FOUND)
     */
    @Transactional
    public void forceDeleteComment(Long commentId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
        if (comment.isDeleted()) {
            return;   // 멱등 — 이미 삭제된 댓글은 중복 처리하지 않음(댓글수 이중 감소도 방지)
        }

        comment.softDelete();
        comment.getPost().decreaseCommentCount();
    }

    // ===== private =====

    /**
     * 게시글을 삭제 여부와 무관하게 조회한다 — 없으면 예외.
     *
     * <p>관리자는 삭제된 글의 상세/댓글도 열람하고, 강제삭제를 멱등으로 처리해야 하므로
     * 사용자 경로의 {@code findDetailById}(활성 전용) 대신 {@code findByIdForAdmin}을 쓴다.
     *
     * @param postId 게시글 PK
     * @return 게시글 엔티티 (삭제 여부 무관, member fetch)
     * @throws BoardException 게시글이 존재하지 않을 때(POST_NOT_FOUND)
     */
    private Post getPostIncludingDeleted(Long postId) {
        return postRepository.findByIdForAdmin(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }

    /**
     * 보드 타입 문자열을 {@link BoardType} enum으로 변환한다.
     *
     * <p>컨트롤러에서 enum 직접 바인딩 시 변환 실패가 Spring 예외(500)로 터져 규격 응답이 불가능하므로,
     * 변환 책임을 서비스가 가진다(PostService.parseBoardType과 동일 정책).
     *
     * @param boardType 보드 타입 문자열
     * @return 변환된 BoardType
     * @throws BoardException 미존재 값일 때(INVALID_BOARD_TYPE, BOARD_003)
     */
    private BoardType parseBoardType(String boardType) {
        try {
            return BoardType.valueOf(boardType);
        } catch (IllegalArgumentException e) {
            throw new BoardException(BoardErrorCode.INVALID_BOARD_TYPE);
        }
    }
}