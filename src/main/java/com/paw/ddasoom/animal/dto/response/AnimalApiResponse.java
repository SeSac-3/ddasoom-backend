package com.paw.ddasoom.animal.dto.response;

import java.util.List;

/**
 * API JSON 구조에 미러링해서 값을 받아옴
 * AnimalApiResponse
 * @param response
 */
public record AnimalApiResponse(ResponseBody response) { 
  /**
   * 
   * ResponseBody
   * @param header
   */
  public record ResponseBody(
    ResponseHeader header
  ) {}
  /**
   * 
   * ResponseHeader
   * @param reqNo
   * @param resultCode
   * @param resultMsg
   */
  public record ResponseHeader(
    long reqNo,
    String resultCode,
    String resultMsg
  ) {}
  /**
   * 
   * Body
   * @param items
   * @param numOfRows
   * @param pageNo
   * @param totalCount
   */
  public record Body(
    Items items,
    int numOfRows,
    int pageNo,
    int totalCount
  ) {}
  /**
   * 
   * Items
   * @param items
   */
  public record Items(
    List<Item> items
  ) {}
  /**
   * 
   * Item
   * @param processState
   * @param desertionNo
   * @param upKindNm
   * @param nickname
   * @param sexCd
   * @param kindNm
   * @param age
   * @param bgnde
   * @param orgNm
   * @param weight
   * @param colorCd
   * @param specialMark
   * @param vaccinationChk
   */
  public record Item(
    String processState,
    String desertionNo,
    String upKindNm,
    String nickname,
    String sexCd,
    String kindNm,
    String age,
    String bgnde,
    String orgNm,
    String weight,
    String colorCd,
    String specialMark,
    String vaccinationChk
  ) {}
}
