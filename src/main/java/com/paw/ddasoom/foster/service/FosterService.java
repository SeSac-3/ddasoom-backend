package com.paw.ddasoom.foster.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalRepository;
import com.paw.ddasoom.common.dto.PageResponse;
import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.domain.FosterStatus;
import com.paw.ddasoom.foster.dto.request.FosterCreateRequest;
import com.paw.ddasoom.foster.dto.request.FosterUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterUserDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterUserListResponse;
import com.paw.ddasoom.foster.exception.FosterErrorCode;
import com.paw.ddasoom.foster.exception.FosterException;
import com.paw.ddasoom.foster.repository.FosterRepository;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FosterService {

        private final FosterRepository fosterRepository;
        private final AnimalRepository animalRepository;
        private final MemberService memberService;

        private static final List<FosterStatus> DUPLICATE_BLOCKING_STATUSES = List.of(
                FosterStatus.PENDING,
                FosterStatus.FOSTERING,
                FosterStatus.EXTENDED
        );

        /** 유저 임시보호 신청 생성 */
        @Transactional
        public void create(Long memberId, FosterCreateRequest request) {

                Animal animal = animalRepository.findById(request.getAnimalId())
                                .orElseThrow(() -> new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND));
                
                validateAnimalAvailableForFoster(animal);

                Member user = memberService.getMember(memberId);
                
                // 동일 유저 동일 동물 중복신청 검증
                validateDuplicateApplication(memberId, request.getAnimalId());

                Foster foster = request.toEntity(animal, user);
                
                fosterRepository.save(foster);

        }

        /** 임시보호 상태(isFostered)인 동물 신청을 막는 검증 메서드  */
        private void validateAnimalAvailableForFoster(Animal animal){
                if(animal.isFostered()){
                   throw new FosterException(FosterErrorCode.ALREADY_FOSTERED_ANIMAL);   
                }
        }

        /**동일 유저의 중복 신청을 막는 검증 메서드 */
        private void validateDuplicateApplication(Long memberId, Long animalId) {
                boolean existsActiveApplication = fosterRepository
                        .existsByUser_IdAndAnimal_IdAndDeletedAtIsNullAndStatusIn(
                        memberId,
                        animalId,
                        DUPLICATE_BLOCKING_STATUSES
                        );

                if (existsActiveApplication) {
                        throw new FosterException(FosterErrorCode.DUPLICATE_FOSTER_APPLICATION);
        }
}

        /** 유저 임시보호신청 수정 */
        @Transactional
        public void update(Long memberId, Long fosterId, FosterUpdateRequest request) {
                Foster foster = fosterRepository.findByIdAndUser_IdAndDeletedAtIsNull(fosterId, memberId)
                                .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

                foster.updateUserRequest(
                                request.getAge(),
                                request.getJob(),
                                request.getMessage());

        }

        /** 유저 글 조회(리스트) */
        @Transactional(readOnly = true)
        public PageResponse<FosterUserListResponse> getFosterList(
                Long memberId,
                FosterStatus status,
                Pageable pageable) {
                Page<Foster> fosterPage = fosterRepository.findAllForUser(memberId, status, pageable);

                return PageResponse.of(fosterPage, FosterUserListResponse::from);
        }

        /** 유저 글 조회(디테일) */
        @Transactional(readOnly = true)
        public FosterUserDetailResponse getFosterDetail(Long memberId, Long fosterId) {
                Foster foster = fosterRepository.findByIdAndUser_IdAndDeletedAtIsNull(fosterId, memberId)
                                .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

                return FosterUserDetailResponse.from(foster);
        }

        /** 유저 글 삭제 */
        @Transactional
        public void delete(Long memberId, Long fosterId) {
                Foster foster = fosterRepository.findByIdAndUser_IdAndDeletedAtIsNull(fosterId, memberId)
                                .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

                foster.softDelete();
        }
}
