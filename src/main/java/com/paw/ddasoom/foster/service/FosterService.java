package com.paw.ddasoom.foster.service;

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
import com.paw.ddasoom.foster.dto.request.FosterCreateRequest;
import com.paw.ddasoom.foster.dto.request.FosterUpdateRequest;
import com.paw.ddasoom.foster.dto.response.FosterUserDetailResponse;
import com.paw.ddasoom.foster.dto.response.FosterUserListResponse;
import com.paw.ddasoom.foster.exception.FosterErrorCode;
import com.paw.ddasoom.foster.exception.FosterException;
import com.paw.ddasoom.foster.repository.FosterRepository;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FosterService {

        private final FosterRepository fosterRepository;
        private final AnimalRepository animalRepository;
        private final MemberRepository memberRepository;

        /** 유저 글 작성 */
        @Transactional
        public void create(Long memberId, FosterCreateRequest request) {

                Animal animal = animalRepository.findById(request.getAnimalId())
                                .orElseThrow(() -> new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND));

                Member user = memberRepository.findById(memberId)
                                .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));

                Foster foster = Foster.create(
                                animal,
                                user,
                                request.getAge(),
                                request.getJob(),
                                request.getMessage());

                fosterRepository.save(foster);

        }

        /** 유저 글 수정 */
        @Transactional
        public void update(Long memberId, Long fosterId, FosterUpdateRequest request) {
                Foster foster = fosterRepository.findByFosterIdAndUser_IdAndDeletedAtIsNull(fosterId, memberId)
                                .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

                foster.updateUserRequest(
                                request.getAge(),
                                request.getJob(),
                                request.getMessage());

        }

        /** 유저 글 조회(리스트) */
        @Transactional(readOnly = true)
        public PageResponse<FosterUserListResponse> getFosterList(Long memberId, Pageable pageable) {
                Page<Foster> fosterPage = fosterRepository
                                .findAllByUser_IdAndDeletedAtIsNullOrderByCreatedAtDesc(memberId, pageable);

                return PageResponse.of(fosterPage, FosterUserListResponse::from);
        }

        /** 유저 글 조회(디테일) */
        @Transactional(readOnly = true)
        public FosterUserDetailResponse getFosterDetail(Long memberId, Long fosterId) {
                Foster foster = fosterRepository.findByFosterIdAndUser_IdAndDeletedAtIsNull(fosterId, memberId)
                                .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

                return FosterUserDetailResponse.from(foster);
        }

        /** 유저 글 삭제 */
        @Transactional
        public void delete(Long memberId, Long fosterId) {
                Foster foster = fosterRepository.findByFosterIdAndUser_IdAndDeletedAtIsNull(fosterId, memberId)
                                .orElseThrow(() -> new FosterException(FosterErrorCode.FOSTER_NOT_FOUND));

                foster.softDelete();
        }
}
