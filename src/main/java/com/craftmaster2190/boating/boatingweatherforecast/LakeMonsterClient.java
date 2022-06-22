package com.craftmaster2190.boating.boatingweatherforecast;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.craftmaster2190.boating.boatingweatherforecast.TomorrowIOClient.AMERICA_DENVER;

@Slf4j
@Component
@RequiredArgsConstructor
public class LakeMonsterClient {
  public static final String LAKE_MONSTER_URI = "https://www.lakemonster.com/lake/";
  public static final Pattern WATER_CONDIITONS_REGEX = Pattern.compile(
      "Water Temp \\(F\\) (?<waterTempLow>\\d+)° - (?<waterTempHigh>\\d+)°");
  public static final DateTimeFormatter DATE_PARSER = DateTimeFormatter.ofPattern("dd-MMM-yy");
  private static final String REDIS_PREFIX = "LakeMonster:";
  private final WebClient webClient;
  private final CacheService cacheService;

  private final ObjectMapper objectMapper;

  public Mono<WaterConditions> getCurrentConditions(UtahLocation utahLocation) {
    val cacheKey = REDIS_PREFIX + utahLocation;
    {
      WaterConditions cachedValue = cacheService.get(cacheKey);
      if (cachedValue != null) {
        return Mono.just(cachedValue);
      }
    }

    val start = Instant.now();
    val today = start.atZone(AMERICA_DENVER).toLocalDate();
    val endDay = today.plusDays(15);
    log.info("START getCurrentConditions({})", utahLocation);
    val uri = LAKE_MONSTER_URI + utahLocation.getLakeMonsterId();
    return webClient.get().uri(uri).retrieve().bodyToMono(String.class).map(Jsoup::parse).map(document -> {
      val script = document
          .select("script")
          .stream()
          .map(Element::html)
          .filter(scriptText -> scriptText.contains("FusionCharts.ready") && scriptText.contains(
              "Water Temperature Estimate"))
          .flatMap(scriptText -> {
            final String data = scriptText.split("let data = ")[1].split(";")[0];

            try {
              return StreamUtils.stream(objectMapper.readTree(data).elements());
            }
            catch (JsonProcessingException e) {
              throw new IllegalStateException(e);
            }
          })
          .map(jsonArray -> {
            val date = jsonArray.get(0).asText();
            LocalDate parsedDate = DATE_PARSER.parse(date, LocalDate::from);
            val tempF = jsonArray.get(2).asDouble();
            return new DatedWaterTemp().setDate(parsedDate).setTempF(tempF);
          })
          .filter(datedWaterTemp -> {
            val dayOfYear = datedWaterTemp.getDate().getDayOfYear();
            return dayOfYear >= today.getDayOfYear() && dayOfYear <= endDay.getDayOfYear();
          })
          .collect(Collectors.groupingBy(datedWaterTemp -> datedWaterTemp.getDate().getDayOfYear(),
              Collectors.averagingDouble(DatedWaterTemp::getTempF)));

      val waterForecast = new ArrayList<DatedWaterTemp>(15);
      for (LocalDate i = today; !i.isAfter(endDay); i = i.plusDays(1)) {
        val averageTempF = script.get(i.getDayOfYear());
        waterForecast.add(new DatedWaterTemp()
            .setDate(i)
            .setTempF(averageTempF)
            .setTempC(Conversions.fahrenheit2celsius(averageTempF)));
      }

      return parseCurrentWaterConditions(document
          .selectFirst("h5:contains(Current Conditions)")
          .nextElementSibling()
          .selectFirst("div:contains(Water Temp)")
          .parent()
          .text()).setWaterForecastTemp(waterForecast);
    }).doOnSuccess(value -> {
      cacheService.put(cacheKey, value);
      log.info("DONE getCurrentConditions({}) {}", utahLocation, Duration.between(start, Instant.now()));
    });
  }

  public WaterConditions parseCurrentWaterConditions(String waterConditionsString) {
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
    private List<DatedWaterTemp> waterForecastTemp;
  }

  @Data
  @Accessors(chain = true)
  public static class DatedWaterTemp {
    private LocalDate date;
    private double tempF;
    private double tempC;
  }

}
