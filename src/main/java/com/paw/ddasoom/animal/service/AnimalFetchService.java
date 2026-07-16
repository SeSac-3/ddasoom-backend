package com.paw.ddasoom.animal.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.paw.ddasoom.animal.dto.response.AnimalFetchResponse;
import com.paw.ddasoom.animal.exception.AnimalErrorCode;
import com.paw.ddasoom.animal.exception.AnimalException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnimalFetchService {

  private final RestClient restClient;

  @Value("${api.service-key}")
  private String serviceKey;

  /**
   * 전체 데이터를 전체 페이지 매핑을 통해 저장
   * @return 모든 동물 리스트
   */
  public List<AnimalFetchResponse.AnimalItem> fetchAnimals() {
    List<AnimalFetchResponse.AnimalItem> allAnimals = new ArrayList<>();

    int pageNo = 1;
    int numOfRows = 1000;

    while (true) {
        AnimalFetchResponse response = fetchPage(pageNo, numOfRows);

        /**
         * 데이터 null check
         */
        if (response.response().body() == null) {
            log.warn("body가 null - pageNo: {}, resultCode 확인 필요", pageNo);
            break;
        }

        /**
         * 데이터 null check
         */
        if (response.response().body().items() == null || response.response().body().items().item() == null) {
            log.info("items 없음(결과 0건 추정) - pageNo: {}", pageNo);
            break;
        }

        List<AnimalFetchResponse.AnimalItem> items = response.response().body().items().item();
        allAnimals.addAll(items);

        // 3. totalCount 꺼내기 전 null 및 공백 방어
        int totalCount = 0;
        String totalCountStr = response.response().body().totalCount();
        if (totalCountStr != null && !totalCountStr.isBlank()) {
            totalCount = Integer.parseInt(totalCountStr.trim());
        } else {
            break; 
        }
        if ((long) pageNo * numOfRows >= totalCount) {
            break;
        }
        pageNo++;
    }
    log.info("총 패치된 동물 수: {}", allAnimals.size());
    return allAnimals;
  }
  
  /**
   * API에서 불러온 데이터를 페이지별로 매핑해서 저장
   */
  public AnimalFetchResponse fetchPage(int pageNo, int numOfRows) {
    AnimalFetchResponse raw = restClient.get()
        .uri(uriBuilder -> uriBuilder
            .path("/abandonmentPublic_v2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", numOfRows)
            .queryParam("pageNo", pageNo)
            .queryParam("_type", "json")
            .build())
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
            throw new AnimalException(AnimalErrorCode.API_NOT_CONNECTED);
        })
        .body(AnimalFetchResponse.class);
    return raw;
  }
}