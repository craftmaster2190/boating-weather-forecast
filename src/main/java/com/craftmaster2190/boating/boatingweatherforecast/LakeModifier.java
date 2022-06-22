package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.*;

@Getter
@RequiredArgsConstructor
public enum LakeModifier {
  CLOSED(1), DOES_NOT_ALLOW_DOGS(2), DOES_NOT_ACCEPT_STATE_PARK_PASS(4);

  private final int modifier;
}
