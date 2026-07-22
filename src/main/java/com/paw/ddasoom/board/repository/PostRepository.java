package com.paw.ddasoom.board.repository;

import com.paw.ddasoom.board.domain.BoardType;
import com.paw.ddasoom.board.domain.Post;
import com.paw.ddasoom.board.dto.projection.AdminPostListProjection;
import com.paw.ddasoom.board.dto.projection.MyPostListProjection;
import com.paw.ddasoom.board.dto.projection.PostListProjection;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 게시글(post) 레포지토리.
 *
 * <p>목록 조회는 카드 UI 전용 projection({@code SELECT NEW})으로 무거운 TEXT 본문을 회피하고,
 * 상세 조회는 {@code JOIN FETCH}로 작성자를 함께 로딩한다.
 *
 * <p>⚠️ {@code SELECT NEW} 뒤의 경로는 반드시 FQCN(전체 클래스 경로) 문자열이어야 한다 —
 * IDE rename이 이 문자열을 갱신하지 않으므로 projection 클래스를 이동/개명하면 수동으로 고쳐야 한다.
 */
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 게시글 목록 조회 — 카드 UI 전용 projection.
     *
     * <p>content 전체 대신 SUBSTRING으로 미리보기만 가져오고(무거운 컬럼 회피),
     * member는 JOIN으로 닉네임만 평면화(N+1 방지). 최신순 정렬.
     *
     * @param boardType 보드 타입 (필수)
     * @param category  카테고리 필터 (NULL이면 전체)
     * @param keyword   제목 부분일치 검색어 (NULL이면 검색 없음)
     * @param pageable  페이지 정보
     * @return 게시글 목록 projection 페이지
     */
    @Query("""
    SELECT NEW com.paw.ddasoom.board.dto.projection.PostListProjection(
        p.id, p.category, p.title, SUBSTRING(p.content, 1, 200),
        p.viewCount, p.commentCount, p.createdAt,
        m.id, m.nickname)
    FROM Post p
    JOIN p.member m
    WHERE p.boardType = :boardType
      AND (:category IS NULL OR p.category = :category)
      AND (:keyword IS NULL OR p.title LIKE CONCAT('%', :keyword, '%'))
      AND p.deletedAt IS NULL
    ORDER BY p.createdAt DESC
    """)
    Page<PostListProjection> findPostList(
            @Param("boardType") BoardType boardType,
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 마이페이지 "내가 쓴 글" 목록 — findPostList와 동일한 projection 전략(무거운 content 회피).
     *
     * <p>여러 보드의 글이 섞여 나오므로 boardType을 함께 조회(프론트 상세 경로 조립용),
     * member 조인은 불필요(본인 글만 조회). 최신순 정렬.
     *
     * @param memberId  조회 대상 회원 PK (본인)
     * @param boardType 보드 타입 필터 — NULL이면 전체 보드 조회 (탭 내 필터의 "전체"와 대응)
     * @param pageable  페이지 정보
     * @return 내가 쓴 글 목록 projection 페이지
     */
    @Query("""
    SELECT NEW com.paw.ddasoom.board.dto.projection.MyPostListProjection(
        p.id, p.boardType, p.category, p.title, SUBSTRING(p.content, 1, 200),
        p.viewCount, p.commentCount, p.createdAt)
    FROM Post p
    WHERE p.member.id = :memberId
      AND (:boardType IS NULL OR p.boardType = :boardType)
      AND p.deletedAt IS NULL
    ORDER BY p.createdAt DESC
    """)
    Page<MyPostListProjection> findMyPosts(
            @Param("memberId") Long memberId,
            @Param("boardType") BoardType boardType,
            Pageable pageable);

    /**
     * [관리자] 게시글 목록 조회 — 전 보드 통합, 삭제된 글 포함.
     *
     * <p>사용자용 {@link #findPostList}와 다른 점:
     * <ul>
     *   <li>{@code p.deletedAt IS NULL} 조건이 없다 — 강제삭제/신고숨김된 글도 목록에 노출해
     *       관리자가 상태를 확인할 수 있게 한다(AdminMember 목록의 탈퇴 회원 노출과 동일 철학).</li>
     *   <li>{@code boardType}이 필수가 아닌 선택 필터 — 전 보드를 한 화면에서 관리한다.</li>
     *   <li>contentPreview를 담지 않는다 — 관리자 목록은 제목 기준 모더레이션이라 불필요.</li>
     * </ul>
     *
     * @param boardType 보드 타입 필터 (NULL이면 전 보드)
     * @param keyword   제목 부분일치 검색어 (NULL이면 검색 없음)
     * @param pageable  페이지 정보
     * @return 관리자 게시글 목록 projection 페이지 (삭제 글 포함)
     */
    @Query("""
    SELECT NEW com.paw.ddasoom.board.dto.projection.AdminPostListProjection(
        p.id, p.boardType, p.category, p.title,
        p.viewCount, p.commentCount, p.createdAt, p.deletedAt,
        m.id, m.nickname)
    FROM Post p
    JOIN p.member m
    WHERE (:boardType IS NULL OR p.boardType = :boardType)
      AND (:keyword IS NULL
           OR p.title LIKE CONCAT('%', :keyword, '%')
           OR m.nickname LIKE CONCAT('%', :keyword, '%'))
    """)
    Page<AdminPostListProjection> findPostsForAdmin(
            @Param("boardType") BoardType boardType,
            @Param("keyword") String keyword,
            Pageable pageable);

    /**
     * 게시글 상세 조회 — member fetch join.
     *
     * <p>단건 조회라 컬렉션 fetch join 페이징 이슈 없음. 삭제된 글은 조회되지 않는다.
     *
     * @param postId 게시글 PK
     * @return 활성 게시글 (작성자 fetch), 없으면 {@code Optional.empty()}
     */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.member
        WHERE p.id = :postId
          AND p.deletedAt IS NULL
        """)
    Optional<Post> findDetailById(@Param("postId") Long postId);

    /**
     * [관리자] 게시글 단건 조회 — 삭제된 글 포함, member fetch join.
     *
     * <p>{@link #findDetailById}와 달리 {@code deletedAt IS NULL} 조건이 없다:
     * 관리자는 이미 삭제된 글의 상세도 열람할 수 있어야 하고(강제삭제 확인·소명 판단),
     * 강제삭제 처리 시 이미 삭제된 대상을 멱등(no-op)으로 처리하려면 삭제 글도 조회돼야 한다.
     *
     * @param postId 게시글 PK
     * @return 게시글 (삭제 여부 무관, 작성자 fetch), 없으면 {@code Optional.empty()}
     */
    @Query("""
        SELECT p FROM Post p
        JOIN FETCH p.member
        WHERE p.id = :postId
        """)
    Optional<Post> findByIdForAdmin(@Param("postId") Long postId);
}