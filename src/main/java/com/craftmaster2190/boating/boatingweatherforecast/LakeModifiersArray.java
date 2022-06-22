package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.Value;

import java.util.Arrays;

@Value
public class LakeModifiersArray {
  private final LakeModifier[] lakeModifiers;

  public LakeModifiersArray(LakeModifier... lakeModifiers) {
    this.lakeModifiers = lakeModifiers;
  }

  public LakeModifiersArray(int lakeModifiers) {
    this.lakeModifiers = Arrays
        .stream(LakeModifier.values())
        .filter(l -> (l.getModifier() & lakeModifiers) != 0)
        .toArray(LakeModifier[]::new);
  }
}
