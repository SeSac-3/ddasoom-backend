package com.paw.ddasoom.animal.dto.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalFetchResponse(
    @JsonProperty("response") Response response
) { 
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Response(
    @JsonProperty("body") Body body
  ) {}
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Body(
    @JsonProperty("items") Items items,
    @JsonProperty("numOfRows") String numOfRows,
    @JsonProperty("pageNo") String pageNo,
    @JsonProperty("totalCount") String totalCount
  ) {}
  @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record Items(
    @JsonProperty("item") List<AnimalItem> item 
  ) {}
  @JsonIgnoreProperties(ignoreUnknown = true)
  public record AnimalItem(
    @JsonProperty("desertionNo") String abandonmentId,
    @JsonProperty("upKindNm") String kind,
    @JsonProperty("sexCd") String gender,
    @JsonProperty("kindNm") String typeName,
    @JsonProperty("age") String age,
    @JsonProperty("orgNm") String location,
    @JsonProperty("weight") String weight,
    @JsonProperty("colorCd") String color,
    @JsonProperty("specialMark") String specialMark,
    @JsonProperty("vaccinationChk") String vaccinationChk,
    @JsonProperty("popfile1") String imageUrl,
    @JsonProperty("bgnde") String rescuedAt
  ) {}
}