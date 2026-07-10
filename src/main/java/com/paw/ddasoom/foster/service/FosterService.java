package com.paw.ddasoom.foster.service;

import org.springframework.stereotype.Service;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalApiRepository;
import com.paw.ddasoom.foster.domain.Foster;
import com.paw.ddasoom.foster.dto.request.FosterCreateRequest;
import com.paw.ddasoom.foster.repository.FosterRepository;
import com.paw.ddasoom.member.domain.Member;
import com.paw.ddasoom.member.exception.MemberErrorCode;
import com.paw.ddasoom.member.exception.MemberException;
import com.paw.ddasoom.member.repository.MemberRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FosterService {

  private final FosterRepository fosterRepository;
  private final AnimalApiRepository animalApiRepository;
  private final MemberRepository memberRepository;

  @Transactional
  public void create(Long memberId, FosterCreateRequest request) {

    Animal animal = animalApiRepository.findById(request.getAnimalId())
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

}
