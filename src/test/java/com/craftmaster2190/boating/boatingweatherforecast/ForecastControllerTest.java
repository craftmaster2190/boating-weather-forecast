package com.craftmaster2190.boating.boatingweatherforecast;

import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

import static com.craftmaster2190.boating.boatingweatherforecast.ForecastController.linearInterpolateToPercent;
import static org.assertj.core.api.Assertions.assertThat;

class ForecastControllerTest {

  @Test
  void test_linearInterpolate() {
    // 50%
    assertThat(linearInterpolateToPercent(0, 100, 50)).isCloseTo(50, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(1, 5, 3)).isCloseTo(50, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(20, 30, 25)).isCloseTo(50, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(0, 90, 45)).isCloseTo(50, Offset.offset(0.1));

    // 75%
    assertThat(linearInterpolateToPercent(10, 30, 25)).isCloseTo(75, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(0, 40, 30)).isCloseTo(75, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(10, 90, 70)).isCloseTo(75, Offset.offset(0.1));

    // 25%
    assertThat(linearInterpolateToPercent(0, 4, 1)).isCloseTo(25, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(0, 40, 10)).isCloseTo(25, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(90, 140, 102.5)).isCloseTo(25, Offset.offset(0.1));

    // 100%
    assertThat(linearInterpolateToPercent(0, 80, 80)).isCloseTo(100, Offset.offset(0.1));
    // 200%
    assertThat(linearInterpolateToPercent(0, 80, 160)).isCloseTo(200, Offset.offset(0.1));
    // -50%
    assertThat(linearInterpolateToPercent(0, 80, -40)).isCloseTo(-50, Offset.offset(0.1));

    // Inverted
    // 25%
    assertThat(linearInterpolateToPercent(4, 0, 3)).isCloseTo(25, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(20, 0, 15)).isCloseTo(25, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(1, 0, 0.75)).isCloseTo(25, Offset.offset(0.1));

    // %50
    assertThat(linearInterpolateToPercent(4, 0, 2)).isCloseTo(50, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(20, 0, 10)).isCloseTo(50, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(1, 0, 0.5)).isCloseTo(50, Offset.offset(0.1));

    // %150
    assertThat(linearInterpolateToPercent(4, 0, -2)).isCloseTo(150, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(20, 0, -10)).isCloseTo(150, Offset.offset(0.1));
    assertThat(linearInterpolateToPercent(100, 50, 25)).isCloseTo(150, Offset.offset(0.1));

    // %-50
    assertThat(linearInterpolateToPercent(4, 0, 6)).isCloseTo(-50, Offset.offset(0.1));
  }
}
