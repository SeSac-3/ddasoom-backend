package com.paw.ddasoom.animal.service;

import org.springframework.stereotype.Service;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;
import com.paw.ddasoom.animal.repository.AnimalRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnimalNicknameService {

  private final AnimalRepository animalRepository;

  @Transactional
  public void updateNickname(Long animalId, String newNickname) {
    Animal animal = animalRepository.findById(animalId)
        .orElseThrow(() -> new AnimalException(AnimalErrorCode.ANIMAL_NOT_FOUND));

        animal.changeNickname(newNickname); // 더티 체킹으로 자동 반영
  }
}
