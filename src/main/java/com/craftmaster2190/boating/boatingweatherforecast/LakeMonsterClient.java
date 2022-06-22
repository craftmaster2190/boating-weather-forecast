package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class LakeMonsterClient {
  public static final String LAKE_MONSTER_URI = "https://www.lakemonster.com/lake/";
  public static final Pattern WATER_CONDIITONS_REGEX = Pattern.compile(
      "Water Temp \\(F\\) (?<waterTempLow>\\d+)° - (?<waterTempHigh>\\d+)°");
  private static final String REDIS_PREFIX = "LakeMonster:";
  private final WebClient webClient;
  private final CacheService cacheService;

  public Mono<WaterConditions> getCurrentConditions(UtahLocation utahLocation) {
    val cacheKey = REDIS_PREFIX + utahLocation;
    {
      WaterConditions cachedValue = cacheService.get(cacheKey);
      if (cachedValue != null) {
        return Mono.just(cachedValue);
      }
    }

    val start = Instant.now();
    log.info("START getCurrentConditions({})", utahLocation);
    return webClient
        .get()
        .uri(LAKE_MONSTER_URI + utahLocation.getLakeMonsterId())
        .retrieve()
        .bodyToMono(String.class)
        .map(Jsoup::parse)
        .map(document -> document.selectFirst("h5:contains(Current Conditions)").nextElementSibling())
        .map(div -> div.selectFirst("div:contains(Water Temp)").parent().text())
        .map(this::parse)
        .doOnSuccess(value -> {
          cacheService.put(cacheKey, value);
          log.info("DONE getCurrentConditions({}) {}", utahLocation, Duration.between(start, Instant.now()));
        });
  }

  public WaterConditions parse(String waterConditionsString) {
    val matcher = WATER_CONDIITONS_REGEX.matcher(waterConditionsString);
    final WaterConditions waterConditions = new WaterConditions().setDescription(waterConditionsString);
    if (!matcher.find()) {
      return waterConditions;
    }
    val waterTempLow = matcher.group("waterTempLow");
    val waterTempHigh = matcher.group("waterTempHigh");
    val highLowF = new HighLow(Double.parseDouble(waterTempHigh), Double.parseDouble(waterTempLow));
    val highLowC = new HighLow(Conversions.fahrenheit2celsius(highLowF.getHigh()),
        Conversions.fahrenheit2celsius(highLowF.getLow()));
    return waterConditions.setHighLowF(highLowF).setHighLowC(highLowC);
  }

  @Data
  @Accessors(chain = true)
  public static class WaterConditions {
    private HighLow highLowC;
    private HighLow highLowF;
    private String description;
  }
}
