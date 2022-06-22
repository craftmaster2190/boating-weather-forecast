package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.*;

import static com.craftmaster2190.boating.boatingweatherforecast.LakeModifier.*;

@Getter
@RequiredArgsConstructor
public enum UtahLocation {
  PINEVIEW("Pineview Reservoir",
      new LakeModifiersArray(DOES_NOT_ACCEPT_STATE_PARK_PASS),
      new LatLong(41.25303093570229, -111.78713551476258),
      "UT/4"),
  DEER_CREEK("Deer Creek Reservoir",
      new LakeModifiersArray(DOES_NOT_ALLOW_DOGS),
      new LatLong(40.447696773355055, -111.4964956293365),
      "UT/5"),
  JORDANELLE("Jordanelle Reservoir",
      new LakeModifiersArray(),
      new LatLong(40.6279009053981, -111.42062908718988),
      "UT/3"),
  WILLARD_BAY("Willard Bay Reservoir",
      new LakeModifiersArray(CLOSED),
      new LatLong(41.410066535418466, -112.0537978706697),
      "UT/30"),
  BEAR_LAKE("Bear Lake Reservoir",
      new LakeModifiersArray(),
      new LatLong(41.9681142615745, -111.39637657332119),
      "UT/8"),
  YUBA("Yuba State Park Reservoir",
      new LakeModifiersArray(),
      new LatLong(39.38219520638649, -112.02496522211041),
      "UT/40"),
  EAST_CANYON("East Canyon Reservoir",
      new LakeModifiersArray(),
      new LatLong(40.924327457944415, -111.59077406597551),
      "UT/42"),
  POWELL_WAHEAP("Lake Powell - Waheap",
      new LakeModifiersArray(DOES_NOT_ACCEPT_STATE_PARK_PASS),
      new LatLong(37.00313530338186, -111.48552420388161),
      "UT/2877"),
  POWELL_BULLFROG("Lake Powell - Bullfrog",
      new LakeModifiersArray(DOES_NOT_ACCEPT_STATE_PARK_PASS),
      new LatLong(37.5179211480762, -110.73966537642387),
      "UT/2878"),
  STRAWBERRY("Strawberry Reservoir",
      new LakeModifiersArray(DOES_NOT_ACCEPT_STATE_PARK_PASS),
      new LatLong(40.179999015373724, -111.15460960661501),
      "UT/130"),
  FLAMING_GORGE("Flaming Gorge",
      new LakeModifiersArray(DOES_NOT_ACCEPT_STATE_PARK_PASS),
      new LatLong(40.98967848094375, -109.59044383452544),
      "UT/11"),
  DEVILS_CREEK("Devil's Creek",
      new LakeModifiersArray(),
      new LatLong(42.29963115623888, -112.20683823881481),
      "ID/2252"),
  SAND_HOLLOW("Sand Hollow Reservoir",
      new LakeModifiersArray(),
      new LatLong(37.12148217972512, -113.38238844032772),
      "UT/29"),
  QUAIL_CREEK("Quail Creek Reservoir",
      new LakeModifiersArray(),
      new LatLong(37.192647896628834, -113.3906789811618),
      "UT/41");

  private final String locationName;
  private final LakeModifiersArray lakeModifiers;
  private final LatLong location;
  private final String lakeMonsterId;
}
