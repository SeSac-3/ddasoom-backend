package com.paw.ddasoom.board.service;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.dto.projection.MyPostListProjection;
import com.paw.ddasoom.board.dto.projection.PostListProjection;
import com.paw.ddasoom.board.dto.request.PostCreateRequest;
import com.paw.ddasoom.board.dto.request.PostUpdateRequest;
import com.paw.ddasoom.board.dto.response.MyPostResponse;
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
import com.paw.ddasoom.common.util.HtmlSanitizer;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 게시글(post) 도메인 서비스.
 *
 * <p>게시글 CRUD와 목록/상세 조회를 담당하며, 이미지 연동은 {@link ImageService}에 위임한다.
 * 트랜잭션 경계는 이 서비스 계층에만 둔다(컨트롤러/레포지토리 금지, CONVENTIONS 6장).
 *
 * <p>설계 원칙 요약:
 * <ul>
 *   <li>{@code boardType}은 컨트롤러에서 {@code String}으로 받아 {@link #parseBoardType(String)}으로
 *       변환한다 — enum 직접 바인딩 시 Spring 예외(500)가 먼저 터져 규격 응답이 불가능하기 때문.</li>
 *   <li>카테고리는 프론트 select 제약을 신뢰하지 않고 서버 화이트리스트로 재검증한다(생성/수정 한정).</li>
 *   <li>삭제는 물리 삭제가 아닌 {@code softDelete}이며, 소유자 삭제 시 이미지도 함께 정리한다.</li>
 * </ul>
 */
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

    /**
     * 조회수 중복 집계 방지 창(window). 같은 회원이 이 시간 안에 같은 글을 여러 번 열어도 1회만 집계.
     * 30분 = 목록↔상세 왕복·새로고침 연타로 인한 인플레는 막되, 하루 여러 번의 정상 재방문은 각각 집계.
     * * (프론트의 staleTime 캐시 대신 서버가 집계 기준을 갖게 하는 것이 목적)
     */
    private static final Duration VIEW_DEDUP_TTL = Duration.ofMinutes(30);

    private final PostRepository postRepository;
    private final MemberRepository memberRepository;
    private final ImageService imageService;
    private final RedisTemplate<String, String> redisTemplate;

    /**
     * 게시글을 생성한다. 카테고리 검증 후 본문을 sanitize하여 저장하고, 임시 업로드된 이미지를 확정 연결한다.
     *
     * <p>이미지 연결 순서: 게시글 INSERT → {@code attach}(imageIds 확정 연결) → 대표 이미지 지정.
     * 대표 이미지는 요청에 {@code thumbnailImageId}가 있을 때만 지정한다.
     *
     * @param memberId 작성자 회원 PK (토큰에서 추출된 값)
     * @param request  게시글 생성 요청 (boardType/category/제목/본문/imageIds/thumbnailImageId)
     * @return 생성된 게시글 PK
     * @throws BoardException  boardType이 유효하지 않거나(BOARD_003) 카테고리가 화이트리스트 밖일 때(BOARD_008)
     * @throws MemberException 회원이 존재하지 않을 때(MEMBER_001)
     */
    @Transactional
    public Long createPost(Long memberId, PostCreateRequest request) {
        BoardType boardType = parseBoardType(request.getBoardType());
        validateCategory(boardType, request.getCategory());
        Member member = getMember(memberId);

        String safeContent = HtmlSanitizer.sanitize(request.getContent());
        Post post = request.toEntity(member, boardType, safeContent);
        postRepository.save(post);

        imageService.attach(request.getImageIds(), OwnerType.POST, post.getId());
        if (request.getThumbnailImageId() != null) {
            imageService.setThumbnail(request.getThumbnailImageId(), OwnerType.POST, post.getId());
        }
        return post.getId();
    }

    /**
     * 게시판 목록을 조회한다(보드/카테고리 필터 + 제목 부분일치 검색, 페이징).
     *
     * <p>무거운 TEXT 본문을 피하기 위해 {@link PostListProjection}으로 조회하며,
     * 목록에 노출할 썸네일 URL은 {@code getThumbnailUrls}로 배치 조회해 N+1을 방지한다.
     *
     * @param boardType 보드 타입 문자열 (필수 — 미존재 값이면 BOARD_003)
     * @param category  카테고리 필터 (선택)
     * @param keyword   제목 부분일치 검색어 (선택 — 빈 문자열/공백은 "검색 없음"으로 정규화)
     * @param pageable  페이지 정보
     * @return 게시글 목록 페이지 (각 항목에 썸네일 URL 포함)
     * @throws BoardException boardType이 유효하지 않을 때(BOARD_003)
     */
    @Transactional(readOnly = true)
    public PageResponse<PostResponse> getPostList(
            String boardType, String category, String keyword, Pageable pageable) {
        BoardType parsedBoardType = parseBoardType(boardType);
        // 빈 문자열/공백 검색어는 "검색 없음"으로 정규화 — 쿼리의 :keyword IS NULL 분기와 일치시킴
        String normalizedKeyword = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
        Page<PostListProjection> page =
                postRepository.findPostList(parsedBoardType, category, normalizedKeyword, pageable);

        List<Long> postIds = page.getContent().stream()
                .map(PostListProjection::getPostId)
                .toList();
        Map<Long, String> thumbnailUrls = imageService.getThumbnailUrls(OwnerType.POST, postIds);

        return PageResponse.of(page, projection ->
                PostResponse.from(projection, thumbnailUrls.get(projection.getPostId())));
    }

    /**
     * 마이페이지 "내가 쓴 글" 목록을 조회한다.
     *
     * <p>{@code boardType}은 선택 필터다 — {@code null}/빈 값이면 전체 보드를 조회한다.
     * 목록 필터는 생성/수정과 달리 엄격 검증 대상이 아니지만, 값이 들어왔는데 미존재 enum이면
     * BOARD_003으로 규격 응답한다. 썸네일 URL은 배치 조회로 N+1을 방지한다.
     *
     * @param memberId  조회 대상 회원 PK (토큰에서 추출된 본인)
     * @param boardType 보드 타입 필터 (선택 — null/빈 값이면 전체)
     * @param pageable  페이지 정보
     * @return 본인 게시글 목록 페이지 (각 항목에 boardType·썸네일 URL 포함)
     * @throws BoardException boardType 값이 있는데 유효하지 않을 때(BOARD_003)
     */
    @Transactional(readOnly = true)
    public PageResponse<MyPostResponse> getMyPosts(Long memberId, String boardType, Pageable pageable) {
        BoardType parsedBoardType =
                boardType != null && !boardType.isBlank() ? parseBoardType(boardType) : null;
        Page<MyPostListProjection> page =
                postRepository.findMyPosts(memberId, parsedBoardType, pageable);

        List<Long> postIds = page.getContent().stream()
                .map(MyPostListProjection::getPostId)
                .toList();
        Map<Long, String> thumbnailUrls = imageService.getThumbnailUrls(OwnerType.POST, postIds);

        return PageResponse.of(page, projection ->
                MyPostResponse.from(projection, thumbnailUrls.get(projection.getPostId())));
    }

    /**
     * 게시글 상세를 조회하고, 뷰어 단위로 중복 제거된 조회수를 집계한다.
     *
     * <p>조회수 증가는 {@link #VIEW_DEDUP_TTL} 창 내 <b>첫 조회일 때만</b> 이루어진다.
     * Redis {@code setIfAbsent}가 원자 연산이라 멀티탭 동시 조회 경합에서도 1회만 통과한다.
     * 조회수 집계 기준을 서버가 갖게 하여, 프론트는 재방문 시 자유롭게 refetch할 수 있다.
     *
     * <p>쓰기(조회수 UPDATE)를 포함하므로 {@code readOnly = true}가 아니다.
     *
     * @param postId   게시글 PK
     * @param viewerId 뷰어 회원 PK (조회수 중복 제거 키의 식별자)
     * @return 게시글 상세 응답 (본문 + 활성 이미지 목록)
     * @throws BoardException 게시글이 없거나 삭제되었을 때(POST_NOT_FOUND)
     */
    @Transactional  // readOnly 아님 — 조회수 증가(쓰기) 포함
    public PostDetailResponse getPostDetail(Long postId, Long viewerId) {
        Post post = getPost(postId);

        // 뷰어 단위 중복 제거: 키가 새로 세팅될 때(=TTL 창 내 첫 조회)만 조회수 증가.
        // setIfAbsent 원자 연산이라 멀티탭 동시 조회 경합에도 1회만 통과.
        Boolean firstView = redisTemplate.opsForValue()
                .setIfAbsent(viewDedupKey(postId, viewerId), "1", VIEW_DEDUP_TTL);
        if (Boolean.TRUE.equals(firstView)) {
            post.increaseViewCount();  // dirty checking으로 UPDATE 반영
        }

        List<ImageResponse> images = imageService.getImages(OwnerType.POST, postId);
        return PostDetailResponse.from(post, images);
    }

    /**
     * 조회수 중복 제거용 Redis 키를 생성한다. 형식: {@code postView:{postId}:{memberId}}.
     * (CONVENTIONS 3장 — {용도}:{식별자} 규칙, 키 생성은 private 메서드로 모음)
     *
     * @param postId   게시글 PK
     * @param viewerId 뷰어 회원 PK
     * @return Redis 키 문자열
     */
    private String viewDedupKey(Long postId, Long viewerId) {
        return "postView:" + postId + ":" + viewerId;
    }

    /**
     * 게시글을 수정한다. 작성자 본인만 수정할 수 있으며, 이미지는 diff 방식으로 동기화한다.
     *
     * <p>{@code syncImages}가 요청 imageIds에 없는 기존 이미지를 soft delete하고 순서를 갱신한다.
     * 대표 이미지는 요청에 {@code thumbnailImageId}가 있을 때만 재지정한다.
     *
     * @param memberId 요청자 회원 PK (작성자 검증용)
     * @param postId   수정 대상 게시글 PK
     * @param request  게시글 수정 요청
     * @throws BoardException boardType 무효(BOARD_003), 카테고리 무효(BOARD_008),
     *                        게시글 없음(POST_NOT_FOUND), 작성자 불일치(POST_ACCESS_DENIED, 403)
     */
    @Transactional
    public void updatePost(Long memberId, Long postId, PostUpdateRequest request) {
        BoardType boardType = parseBoardType(request.getBoardType());
        validateCategory(boardType, request.getCategory());
        Post post = getPost(postId);
        validateAuthor(post, memberId);

        post.update(boardType, request.getCategory(),
                request.getTitle(), HtmlSanitizer.sanitize(request.getContent()));

        // 수정은 diff 동기화 — 빠진 이미지 soft delete + 순서 갱신 (IMAGE_FLOW 3-6)
        imageService.syncImages(request.getImageIds(), OwnerType.POST, postId);
        if (request.getThumbnailImageId() != null) {
            imageService.setThumbnail(request.getThumbnailImageId(), OwnerType.POST, postId);
        }
    }

    /**
     * 게시글을 soft delete하고, 소유자에 연결된 이미지를 일괄 정리한다.
     *
     * <p>이미지 정리는 빈 리스트로 {@code syncImages}를 호출하는 패턴을 재사용한다(IMAGE_FLOW 3-6).
     *
     * @param memberId 요청자 회원 PK (작성자 검증용)
     * @param postId   삭제 대상 게시글 PK
     * @throws BoardException 게시글 없음(POST_NOT_FOUND), 작성자 불일치(POST_ACCESS_DENIED, 403)
     */
    @Transactional
    public void deletePost(Long memberId, Long postId) {
        Post post = getPost(postId);
        validateAuthor(post, memberId);

        post.softDelete();
        // 소유자 삭제 시 이미지 일괄 정리 = 빈 리스트 sync (IMAGE_FLOW 3-6 패턴)
        imageService.syncImages(List.of(), OwnerType.POST, postId);
    }

    // ===== private =====

    /**
     * 활성 게시글을 조회한다 — 없거나 삭제되었으면 예외.
     *
     * @param postId 게시글 PK
     * @return 게시글 엔티티 (member fetch join)
     * @throws BoardException 게시글이 없거나 삭제되었을 때(POST_NOT_FOUND)
     */
    private Post getPost(Long postId) {
        return postRepository.findDetailById(postId)
                .orElseThrow(() -> new BoardException(BoardErrorCode.POST_NOT_FOUND));
    }

    /**
     * 회원을 조회한다 — 없으면 예외.
     *
     * @param memberId 회원 PK
     * @return 회원 엔티티
     * @throws MemberException 회원이 존재하지 않을 때(MEMBER_001)
     */
    private Member getMember(Long memberId) {
        return memberRepository.findById(memberId)
                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
    }

    /**
     * 보드 타입별 카테고리 화이트리스트로 카테고리를 검증한다.
     *
     * @param boardType 보드 타입
     * @param category  검증 대상 카테고리
     * @throws BoardException 화이트리스트에 없는 카테고리일 때(INVALID_CATEGORY, BOARD_008)
     */
    private void validateCategory(BoardType boardType, String category) {
        Set<String> allowed = CATEGORY_WHITELIST.get(boardType);
        if (allowed == null || !allowed.contains(category)) {
            throw new BoardException(BoardErrorCode.INVALID_CATEGORY);  // BOARD_008 신규 추가 필요
        }
    }

    /**
     * 게시글 작성자 본인인지 검증한다.
     *
     * <p>⚠️ {@code Long}은 {@code ==} 비교 금지(캐시 범위 -128~127 밖에서는 항상 false) — {@code equals} 필수.
     *
     * @param post     대상 게시글
     * @param memberId 요청자 회원 PK
     * @throws BoardException 작성자가 아닐 때(POST_ACCESS_DENIED, 403)
     */
    private void validateAuthor(Post post, Long memberId) {
        // ⚠️ Long은 == 비교 금지 (128 초과 시 항상 false) — equals 필수
        if (!post.getMember().getId().equals(memberId)) {
            throw new BoardException(BoardErrorCode.POST_ACCESS_DENIED);  // BOARD_002, 403
        }
    }

    /**
     * 보드 타입 문자열을 {@link BoardType} enum으로 변환한다.
     *
     * <p>컨트롤러에서 {@code BoardType}으로 직접 바인딩하면 변환 실패 시 Spring 예외(500)가 먼저 터져
     * BOARD_003(400) 규격 응답이 불가능하므로, 변환 책임을 서비스가 가진다.
     *
     * @param boardType 보드 타입 문자열
     * @return 변환된 BoardType
     * @throws BoardException null이거나 미존재 값일 때(INVALID_BOARD_TYPE, BOARD_003)
     */
    private BoardType parseBoardType(String boardType) {
        if (boardType == null) {
            throw new BoardException(BoardErrorCode.INVALID_BOARD_TYPE); // BOARD_003
        }
        try {
            return BoardType.valueOf(boardType);
        } catch (IllegalArgumentException e) {
            throw new BoardException(BoardErrorCode.INVALID_BOARD_TYPE);
        }
    }
}