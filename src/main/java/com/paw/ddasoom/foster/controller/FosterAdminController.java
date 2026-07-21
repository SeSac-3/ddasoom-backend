package com.paw.ddasoom.foster.controller;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.paw.ddasoom.common.dto.ApiResponse;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.common.security.CustomUserDetails;
import com.paw.ddasoom.foster.domain.FosterManagementScope;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.dto.request.FosterAdminUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterAdminDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterAdminListResponse;
import com.paw.ddasoom.foster.service.FosterAdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@Tag(
    name = "관리자 임시보호(Admin Foster)",
    description = "관리자 임시보호 신청·진행 관리 API"
)
@SecurityRequirement(name = "bearerAuth")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin/fosters")
public class FosterAdminController {

  private final FosterAdminService fosterAdminService;

  @Operation(
      summary = "관리자 임시보호 신청 상세 조회",
      description = """
          관리자용 임시보호 신청 상세 정보를 조회합니다.
          삭제된 신청도 조회할 수 있습니다.
          - 인가: ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "조회 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "임시보호 신청 없음(FOSTER_001)"
      )
  })
  @GetMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<FosterAdminDetailResponse>> getFosterDetail(
      @Parameter(description = "임시보호 신청 PK", required = true, example = "1")
      @PathVariable("fosterId") Long fosterId
  ) {
    FosterAdminDetailResponse response = fosterAdminService.getFosterDetail(fosterId);

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(
      summary = "관리자 임시보호 신청 목록 조회",
      description = """
          관리 범위와 상태, 삭제 여부, 신청일 기간으로 임시보호 신청을 조회합니다.
          - APPLICATION: 신청 관리(PENDING, REJECTED)
          - PROGRESS: 임시보호 진행 관리(FOSTERING, EXTENDED, ENDED)
          - activeOnly=true: FOSTERING, EXTENDED 상태만 조회
          - page는 0부터 시작
          - 인가: ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "조회 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = "잘못된 조회 조건(FOSTER_005), 잘못된 날짜 범위(FOSTER_006)"
      )
  })
  @GetMapping
  public ResponseEntity<ApiResponse<PageResponse<FosterAdminListResponse>>> getFosterList(
      @Parameter(
          description = "관리 범위: APPLICATION(신청 관리), PROGRESS(보호 진행 관리)",
          required = true,
          example = "APPLICATION"
      )
      @RequestParam("scope") FosterManagementScope scope,

      @Parameter(
          description = "상태 필터. 미입력 시 scope에 해당하는 전체 상태 조회",
          example = "PENDING"
      )
      @RequestParam(value = "status", required = false) FosterStatus status,

      @Parameter(
          description = "true면 FOSTERING, EXTENDED 상태만 조회",
          example = "false"
      )
      @RequestParam(value = "activeOnly", defaultValue = "false") boolean activeOnly,

      @Parameter(
          description = "true면 소프트 삭제된 신청도 포함",
          example = "false"
      )
      @RequestParam(value = "includeDeleted", defaultValue = "false") boolean includeDeleted,

      @Parameter(
          description = "신청일 조회 시작일(yyyy-MM-dd)",
          example = "2026-07-01"
      )
      @RequestParam(value = "startDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

      @Parameter(
          description = "신청일 조회 종료일(yyyy-MM-dd)",
          example = "2026-07-31"
      )
      @RequestParam(value = "endDate", required = false)
      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

      @Parameter(description = "페이지 번호(0부터 시작)", example = "0")
      @RequestParam(value = "page", defaultValue = "0")
      @Min(value = 0, message = "페이지 번호는 0 이상이어야 합니다.")
      int page,

      @Parameter(description = "페이지 크기(1~500)", example = "20")
      @RequestParam(value = "size", defaultValue = "20")
      @Min(value = 1, message = "페이지 크기는 1 이상이어야 합니다.")
      @Max(value = 500, message = "페이지 크기는 500 이하여야 합니다.")
      int size
  ) {
    PageResponse<FosterAdminListResponse> response = fosterAdminService.getFosterList(
        scope,
        status,
        activeOnly,
        includeDeleted,
        startDate,
        endDate,
        PageRequest.of(page, size)
    );

    return ResponseEntity.ok(ApiResponse.success(response));
  }

  @Operation(
      summary = "관리자 임시보호 신청 처리 및 수정",
      description = """
          관리자 답변, 신청 상태, 임시보호 일정을 수정합니다.
          기존 답변과 일정 정보를 모두 포함해 보내는 전체 상태 전송 방식입니다.
          종료(ENDED) 상태인 신청의 일정은 수정할 수 없습니다.
          - 인가: ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "수정 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = """
                이미 삭제된 신청(FOSTER_003), 허용되지 않은 상태 전이(FOSTER_007),
                일정 오류(FOSTER_008), 전체 값 누락(FOSTER_013),
                필수 일정 누락(FOSTER_014), 종료된 신청 일정 변경(FOSTER_017),
                다른 활성 임시보호 신청 존재(FOSTER_018)
                """
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "임시보호 신청 없음(FOSTER_001)"
      )
  })
  @PatchMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> update(
      @AuthenticationPrincipal CustomUserDetails userDetails,
      @Parameter(description = "임시보호 신청 PK", required = true, example = "1")
      @PathVariable("fosterId") Long fosterId,
      @Valid @RequestBody FosterAdminUpdateRequest request
  ) {
    fosterAdminService.updateFoster(userDetails.getMemberId(), fosterId, request);

    return ResponseEntity.ok(ApiResponse.success("임시보호 신청 처리 정보가 수정되었습니다."));
  }

  @Operation(
      summary = "관리자 임시보호 신청 삭제",
      description = """
          관리자 권한으로 임시보호 신청을 소프트 삭제합니다.
          신청 거절(REJECTED) 또는 종료(ENDED) 상태만 삭제할 수 있습니다.
          신청 대기(PENDING)와 임시보호 진행 상태(FOSTERING, EXTENDED)는 삭제할 수 없습니다.
          - 인가: ADMIN
          """
  )
  @ApiResponses({
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "200",
          description = "삭제 성공"
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "400",
          description = """
              이미 삭제된 신청(FOSTER_003), 신청 대기 삭제 불가(FOSTER_015),
              임시보호 진행 상태 삭제 불가(FOSTER_016)
              """
      ),
      @io.swagger.v3.oas.annotations.responses.ApiResponse(
          responseCode = "404",
          description = "임시보호 신청 없음(FOSTER_001)"
      )
  })
  @DeleteMapping("/{fosterId}")
  public ResponseEntity<ApiResponse<Void>> delete(
      @Parameter(description = "임시보호 신청 PK", required = true, example = "1")
      @PathVariable("fosterId") Long fosterId
  ) {
    fosterAdminService.deleteFoster(fosterId);

    return ResponseEntity.ok(
        ApiResponse.success("임시보호 신청이 삭제되었습니다.")
    );
  }
}