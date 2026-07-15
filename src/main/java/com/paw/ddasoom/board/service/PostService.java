package com.paw.ddasoom.board.service;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.dto.projection.PostListProjection;
import com.paw.ddasoom.board.dto.request.PostCreateRequest;
import com.paw.ddasoom.board.dto.request.PostUpdateRequest;
import com.paw.ddasoom.board.dto.response.PostDetailResponse;
import com.paw.ddasoom.board.dto.response.PostResponse;
import com.paw.ddasoom.board.exception.BoardErrorCode;
import com.paw.ddasoom.board.exception.BoardException;
import com.paw.ddasoom.board.repository.PostRepository;
import com.paw.ddasoom.image.domain.OwnerType;
import com.paw.ddasoom.image.dto.response.ImageResponse;
import com.paw.ddasoom.image.service.ImageService;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;
import com.paw.ddasoom.common.dto.PageResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostService {

    /**
     * 카테고리 화이트리스트 — 프론트 select 제약은 신뢰 대상이 아니므로 서버에서 재검증.
     * 생성/수정 시에만 엄격 검증. 목록 조회 필터는 검증하지 않음 (미존재 값 → 자연히 빈 결과).
     */
    private static final Set<String> ADOPTION_REVIEW_CATEGORIES = Set.of("강아지", "고양이");
    private static final Set<String> DOG_INFO_CATEGORIES = Set.of("예방접종");
    private static final Set<String> CAT_INFO_CATEGORIES = Set.of("예방접종");

    private static final Map<BoardType, Set<String>> CATEGORY_WHITELIST = Map.of(
            BoardType.ADOPTION_REVIEW, ADOPTION_REVIEW_CATEGORIES,
            BoardType.DOG_INFO, DOG_INFO_CATEGORIES,
            BoardType.CAT_INFO, CAT_INFO_CATEGORIES
    );

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;

    @Transactional
    public Long createPost(Long memberId, PostCreateRequest request) {
        BoardType boardType = parseBoardType(request.getBoardType());
        validateCategory(boardType, request.getCategory());
        Member member = getMember(memberId);

        Post post = request.toEntity(member, boardType);
        postRepository.save(post);

        imageService.attach(request.getImageIds(), OwnerType.POST, post.getId());
        if (request.getThumbnailImageId() != null) {
            imageService.setThumbnail(request.getThumbnailImageId(), OwnerType.POST, post.getId());
        }
        return post.getId();
    }

    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostList(String boardType, String category, Pageable pageable) {
        BoardType parsedBoardType = parseBoardType(boardType);
        Page<PostListProjection> page = postRepository.findPostList(parsedBoardType, category, pageable);

        List<Long> postIds = page.getContent().stream()
                .map(PostListProjection::getPostId)
                .toList();
        Map<Long, String> thumbnailUrls = imageService.getThumbnailUrls(OwnerType.POST, postIds);

        return PageResponse.of(page, projection ->
                PostResponse.from(projection, thumbnailUrls.get(projection.getPostId())));
    }

    @Transactional  // readOnly 아님 — 조회수 증가(쓰기) 포함
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = getPost(postId);
        post.increaseViewCount();  // dirty checking으로 UPDATE 반영

        List<ImageResponse> images = imageService.getImages(OwnerType.POST, postId);
        return PostDetailResponse.from(post, images);
    }

    @Transactional
    public void updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        BoardType boardType = parseBoardType(request.getBoardType());
        validateCategory(boardType, request.getCategory());
        Post post = getPost(postId);
        validateAuthor(post, memberId);

        post.update(boardType, request.getCategory(),
                request.getTitle(), request.getContent());

        // 수정은 diff 동기화 — 빠진 이미지 soft delete + 순서 갱신 (IMAGE_FLOW 3-6)
        imageService.syncImages(request.getImageIds(), OwnerType.POST, postId);
        if (request.getThumbnailImageId() != null) {
            imageService.setThumbnail(request.getThumbnailImageId(), OwnerType.POST, postId);
        }
    }

    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = getPost(postId);
        validateAuthor(post, memberId);

        post.softDelete();
        // 소유자 삭제 시 이미지 일괄 정리 = 빈 리스트 sync (IMAGE_FLOW 3-6 패턴)
        imageService.syncImages(List.of(), OwnerType.POST, postId);
    }

    // ===== private =====

    private Post getPost(Long postId) {
        return postRepository.findDetailById(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }

    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    private void validateCategory(BoardType boardType, String category) {
        Set<String> allowed = CATEGORY_WHITELIST.get(boardType);
        if (allowed == null || !allowed.contains(category)) {
            throw new BoardException(BoardErrorCode.INVALID_CATEGORY);  // BOARD_008 신규 추가 필요
        }
    }

    private void validateAuthor(Post post, Long memberId) {
        // ⚠️ Long은 == 비교 금지 (128 초과 시 항상 false) — equals 필수
        if (!post.getMember().getId().equals(memberId)) {
            throw new BoardException(BoardErrorCode.POST_ACCESS_DENIED);  // BOARD_002, 403
        }
    }

    /** String → enum 변환 — 컨트롤러에서 BoardType으로 직접 바인딩하면 Spring 예외(500)가 먼저 터져
     BOARD_003(400) 규격 응답이 불가능하므로, 변환 책임을 서비스가 가짐 */
    private BoardType parseBoardType(String boardType) {
        try {
            return BoardType.valueOf(boardType);
        } catch (IllegalArgumentException e) {
            throw new BoardException(BoardErrorCode.INVALID_BOARD_TYPE);
        }
    }
}