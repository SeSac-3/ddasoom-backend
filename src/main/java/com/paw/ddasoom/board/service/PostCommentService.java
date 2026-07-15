package com.paw.ddasoom.board.service;

import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.domain.PostComment;
import com.paw.ddasoom.board.dto.request.CommentCreateRequest;
import com.paw.ddasoom.board.dto.request.CommentUpdateRequest;
import com.paw.ddasoom.board.dto.response.CommentResponse;
import com.paw.ddasoom.board.dto.projection.CommentListProjection;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.board.exception.BoardErrorCode;
import com.paw.ddasoom.board.exception.BoardException;
import com.paw.ddasoom.board.repository.PostCommentRepository;
import com.paw.ddasoom.board.repository.PostRepository;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostCommentService {

    private final PostCommentRepository postCommentRepository;
    private final PostRepository postRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public CommentResponse create(Long postId, Long memberId, CommentCreateRequest request) {
        Post post = getActivePost(postId);
        Member member = getMember(memberId);

        PostComment comment = PostComment.builder()
                .post(post)
                .member(member)
                .content(request.getContent())
                .build();
        postCommentRepository.save(comment);

        post.increaseCommentCount();

        return CommentResponse.from(comment);
    }

    @Transactional(readOnly = true)
    public PageResponse<CommentResponse> getComments(Long postId, int page, int size) {
        getActivePost(postId);   // 존재 확인 (없으면 POST_NOT_FOUND)

        Pageable pageable = PageRequest.of(page, size);
        Page<CommentListProjection> result =
                postCommentRepository.findCommentsByPostId(postId, pageable);

        return PageResponse.of(result, CommentResponse::from);
    }

    @Transactional
    public CommentResponse update(Long postId, Long commentId, Long memberId, CommentUpdateRequest request) {
        PostComment comment = getActiveComment(commentId, postId);
        validateAuthor(comment, memberId);

        comment.updateContent(request.getContent());

        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long postId, Long commentId, Long memberId) {
        PostComment comment = getActiveComment(commentId, postId);
        validateAuthor(comment, memberId);

        comment.softDelete();

        Post post = comment.getPost();
        post.decreaseCommentCount();
    }

    private Post getActivePost(Long postId) {
        return postRepository.findDetailById(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private PostComment getActiveComment(Long commentId, Long postId) {
        return postCommentRepository.findByIdAndPostIdAndDeletedAtIsNull(commentId, postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.COMMENT_NOT_FOUND));
    }

    private void validateAuthor(PostComment comment, Long memberId) {
        if (!comment.getMember().getId().equals(memberId)) {
            throw new BoardException(BoardErrorCode.COMMENT_ACCESS_DENIED);
        }
    }
}