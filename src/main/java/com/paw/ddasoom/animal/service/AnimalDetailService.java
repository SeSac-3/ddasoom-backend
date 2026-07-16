package com.paw.ddasoom.animal.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.dto.response.AnimalDetailPageResponse;
import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalLikeRepository;
import com.paw.ddasoom.animal.repository.AnimalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnimalDetailService {

  private final AnimalRepository animalRepository;
  private final AnimalLikeRepository animalLikeRepository;

  // memberId: 비로그인이면 null -> isLiked=false
  @Transactional(readOnly = true)
  public AnimalDetailPageResponse getDetail(Long animalId, Long memberId) {
    Animal animal = animalRepository.findById(animalId)
      .orElseThrow(() -> new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND));

      boolean isLiked = memberId != null
        && animalLikeRepository.existsByAnimal_IdAndMember_Id(animalId, memberId);

      return AnimalDetailPageResponse.from(animal, isLiked);
  }
}
