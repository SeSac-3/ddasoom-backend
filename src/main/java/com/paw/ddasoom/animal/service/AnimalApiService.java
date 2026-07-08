package com.paw.ddasoom.animal.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.paw.ddasoom.animal.domain.Animal;
import com.paw.ddasoom.animal.repository.AnimalApiRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AnimalApiService {

  private final RestClient restClient;
  private final AnimalApiRepository apiRepository;

  public void fetchAnimals() {
    List<Animal> allAnimals = new ArrayList<>();

    int numOfRows = 1000; // API에서 불러올 수 있는 최대 열의 갯수
    int pageNo = 1; // 초기 페이지 숫자
    int totalCount; // 총 열의 갯수


  }

  public void fetchPage(int pageNo, int numOfRows) {
    
  }
}
