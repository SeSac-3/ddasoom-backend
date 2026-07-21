package com.paw.ddasoom.foster.dto.response;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class FosterPendingApplicationResponse {
  private final boolean hasPendingApplication;
}
