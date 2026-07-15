package com.paw.ddasoom.animal.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.dto.response.AnimalListPageResponse;
import com.paw.ddasoom.animal.repository.AnimalRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AnimalListPageService {

  private final AnimalRepository animalRepository;

  @Transactional(readOnly = true)
  public List<AnimalListPageResponse> findAllAnimals() {
    List<Animal> foundAnimals =  animalRepository.findAll();
    List<AnimalListPageResponse> response = foundAnimals.stream()
            .map(AnimalListPageResponse::from)
            .toList();
    return response;
  } 
}
