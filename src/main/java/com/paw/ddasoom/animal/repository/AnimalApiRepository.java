package com.paw.ddasoom.animal.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.paw.ddasoom.animal.domain.Animal;

public interface AnimalApiRepository extends JpaRepository<Animal, Long> {

}
