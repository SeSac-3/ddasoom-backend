package com.paw.ddasoom.foster.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.dto.request.FosterCreateRequest;
import com.paw.ddasoom.foster.dto.request.FosterUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterPendingApplicationResponse;
import com.paw.ddasoom.foster.dto.response.FosterUserDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterUserListResponse;
import com.paw.ddasoom.foster.service.FosterService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * 일반 사용자의 임시보호 신청 생성·조회·수정·삭제 API를 제공한다.
 *
 * <p>모든 API는 USER 또는 ADMIN 권한이 필요하다.</p>
 */
@Tag(
    name = "임시보호(Foster)",
    description = "사용자 임시보호 신청·조회·수정·삭제 API"
)
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/fosters")
public class FosterController {

  private final FosterService fosterService;

  /**
   * 현재 사용자의 해당 동물 신청 대기 상태 여부를 조회한다.
   */
  @Operation(
      summary = "내 동물의 신청 대기 임시보호 신청 여부 조회",
      description = """
          현재 로그인한 사용자가 해당 동물에 대해 삭제되지 않은 신청 대기(PENDING) 상태의
          임시보호 신청을 가지고 있는지 조회합니다.

          - 조회 기준: 로그인 사용자 + 동물 PK
          - 포함 상태: PENDING
          - 제외 상태: REJECTED, FOSTERING, EXTENDED, ENDED, Soft Delete
          - FOSTERING, EXTENDED 상태는 동물 상세 응답의 animal.isFostered로 별도 판단합니다.
          - 인가: USER, ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "조회 성공"
      )
  })
  @GetMapping("/my/pending")
  public ResponseEntity<ApiResponse<FosterPendingApplicationResponse>> getPendingApplicationStatus(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "동물 PK", required = true, example = "1")
      @RequestParam("animalId") Long animalId
  ) {
    FosterPendingApplicationResponse response = fosterService.getFosterPendingApplicationStatus(
        userDetails.getMemberId(),
        animalId
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 로그인한 사용자의 임시보호 신청을 생성한다.
   */
  @Operation(
      summary = "임시보호 신청",
      description = """
          로그인한 사용자가 유기동물 임시보호를 신청합니다.

          - 동일 사용자는 같은 동물에 PENDING, FOSTERING, EXTENDED 상태 신청을 중복 생성할 수 없습니다.
          - 이미 임시보호 중인 동물은 신청할 수 없습니다.
          - REJECTED, ENDED, Soft Delete 상태의 과거 신청이 있으면 재신청할 수 있습니다.
          - 인가: USER, ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "201",
          description = "신청 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "중복 신청(FOSTER_009), 이미 임시보호 중인 동물(FOSTER_010)"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "동물 없음(ANIMAL_001)"
      )
  })
  @PostMapping
  public ResponseEntity<ApiResponse<Void>> create(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Valid @RequestBody FosterCreateRequest request
  ) {
    fosterService.create(userDetails.getMemberId(), request);

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("임시보호 신청이 완료되었습니다."));
  }

  /**
   * 신청 대기 상태의 본인 임시보호 신청을 수정한다.
   */
  @Operation(
      summary = "내 임시보호 신청 수정",
      description = """
          신청 대기(PENDING) 상태의 본인 임시보호 신청만 수정합니다.

          - 수정 가능 항목: 나이, 직업, 하고 싶은 말
          - 타인의 신청은 존재하지 않는 신청으로 처리합니다.
          - 인가: USER, ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "수정 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "신청 대기 상태가 아닌 경우(FOSTER_011)"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "신청 없음 또는 접근 불가(FOSTER_001)"
      )
  })
  @PatchMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "임시보호 신청 PK", required = true, example = "1")
      @PathVariable("fosterId") Long fosterId,
      @Valid @RequestBody FosterUpdateRequest request
  ) {
    fosterService.update(userDetails.getMemberId(), fosterId, request);

    return ResponseEntity.ok(ApiResponse.success("임시보호 신청이 수정되었습니다."));
  }

  /**
   * 현재 사용자의 임시보호 신청 목록을 조회한다.
   */
  @Operation(
      summary = "내 임시보호 신청 목록 조회",
      description = """
          현재 로그인한 사용자의 임시보호 신청 목록을 상태별·페이지별로 조회합니다.

          - 삭제된 신청은 목록에서 제외됩니다.
          - page는 0부터 시작합니다.
          - 인가: USER, ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "조회 성공"
      )
  })
  @GetMapping("/my")
  public ResponseEntity<ApiResponse<PageResponse<FosterUserListResponse>>> getFosterList(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(
          description = "상태 필터. 미입력 시 전체 상태 조회",
          example = "PENDING"
      )
      @RequestParam(value = "status", required = false) FosterStatus status,
      @Parameter(description = "페이지 번호. 0부터 시작", example = "0")
      @RequestParam(value = "page", defaultValue = "0") int page,
      @Parameter(description = "페이지 크기", example = "20")
      @RequestParam(value = "size", defaultValue = "20") int size
  ) {
    PageResponse<FosterUserListResponse> response = fosterService.getFosterList(
        userDetails.getMemberId(),
        status,
        PageRequest.of(page, size)
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 현재 사용자의 임시보호 신청 상세를 조회한다.
   */
  @Operation(
      summary = "내 임시보호 신청 상세 조회",
      description = """
          현재 로그인한 사용자의 임시보호 신청 상세 정보를 조회합니다.

          - 타인의 신청은 존재하지 않는 신청으로 처리합니다.
          - 삭제된 신청은 조회할 수 없습니다.
          - 인가: USER, ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "조회 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "신청 없음 또는 접근 불가(FOSTER_001)"
      )
  })
  @GetMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<FosterUserDetailResponse>> getFosterDetail(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "임시보호 신청 PK", required = true, example = "1")
      @PathVariable("fosterId") Long fosterId
  ) {
    FosterUserDetailResponse response = fosterService.getFosterDetail(
        userDetails.getMemberId(),
        fosterId
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * 본인 임시보호 신청을 소프트 삭제한다.
   */
  @Operation(
      summary = "내 임시보호 신청 삭제",
      description = """
          본인의 임시보호 신청을 소프트 삭제합니다.

          - 신청 대기(PENDING), 신청 거절(REJECTED) 상태만 삭제할 수 있습니다.
          - FOSTERING, EXTENDED, ENDED 상태는 사용자 삭제가 불가능합니다.
          - 타인의 신청은 존재하지 않는 신청으로 처리합니다.
          - 인가: USER, ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "삭제 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "삭제 불가 상태(FOSTER_012)"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "신청 없음 또는 접근 불가(FOSTER_001)"
      )
  })
  @DeleteMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "임시보호 신청 PK", required = true, example = "1")
      @PathVariable("fosterId") Long fosterId
  ) {
    fosterService.delete(userDetails.getMemberId(), fosterId);

    return ResponseEntity.ok(ApiResponse.success("임시보호 신청이 삭제되었습니다."));
  }
}