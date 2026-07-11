package com.paw.ddasoom.animal.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.animal.domain.Animal;

public interface AnimalRepository extends JpaRepository<Animal, Long> {
  Optional<Animal> findByAbandonmentId(String abandonmentId);
}
