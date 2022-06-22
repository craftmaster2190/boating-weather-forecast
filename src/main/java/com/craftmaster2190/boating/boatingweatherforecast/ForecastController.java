package com.craftmaster2190.boating.boatingweatherforecast;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.*;
import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.*;
import java.util.stream.*;

@Slf4j
@RestController
@RequiredArgsConstructor
public class ForecastController {
  private final LakeMonsterClient lakeMonsterClient;
  private final TomorrowIOClient tomorrowIOClient;
  private final ObjectMapper objectMapper;

  public static void forForecasts(List<TomorrowIOClient.Forecast> forecasts, BiConsumer<HighLow, HighLow> consumerCAndF) {
    Function<ToDoubleFunction<TomorrowIOClient.Forecast>, DoubleStream> toDoubleStream = (func) -> forecasts
        .stream()
        .mapToDouble(func);

    Function<ToDoubleFunction<TomorrowIOClient.Forecast>, HighLow> toHighLow = (func) -> new HighLow(toDoubleStream
        .apply(func)
        .max()
        .orElseThrow(IllegalStateException::new),
        toDoubleStream.apply(func).min().orElseThrow(IllegalStateException::new));

    consumerCAndF.accept(toHighLow.apply(TomorrowIOClient.Forecast::getTemperatureC),
        toHighLow.apply(TomorrowIOClient.Forecast::getTemperatureF));
  }

  public static double linearInterpolateToPercent(double low, double high, double value) {
    // low < value < high => 0 < y < 100
    //  x1     x      x2     y1  y   y2
    // y = y1+ (((x-x1) * (y2-y1)) / (x2-x1))

    return 0 + (((value - low) * (100.0 - 0)) / (high - low));
  }

  @GetMapping("/fetch/{location}")
  public Mono<ForecastResponse> fetch(@PathVariable("location") UtahLocation utahLocation) {
    val forecastResponse = new ForecastResponse().setUtahLocation(utahLocation);

    return Flux
        .concat(Flux
                .concat(tomorrowIOClient.getForecast(utahLocation, TomorrowIOClient.Timestep.EVERY_HOUR),
                    tomorrowIOClient.getForecast(utahLocation, TomorrowIOClient.Timestep.EVERY_DAY))
                .collectList()
                .map(listOfLists -> listOfLists.stream().flatMap(Collection::stream).collect(Collectors.toList()))
                .doOnSuccess(forecastResponse::setWeatherForecast),
            lakeMonsterClient.getCurrentConditions(utahLocation).doOnSuccess(forecastResponse::setWaterConditions))
        .collectList()
        .thenReturn(forecastResponse);
  }

  @GetMapping("/best")
  public Mono<Map<LocalDate, List<ScoredForecastResponse>>> getBestPlacesToGo(
      @RequestParam double latitude,
      @RequestParam double longitude,
      @RequestParam(defaultValue = "5", required = false, name = "results") int maxResultsPerDay) {
    log.info("getBestPlacesToGo({}, {}, {})", latitude, longitude, maxResultsPerDay);
    return Flux
        .fromStream(Arrays.stream(UtahLocation.values()).map(utahLocation -> {
          val distanceKilometers = Conversions.distanceInKilometers(new LatLong(latitude, longitude),
              utahLocation.getLocation());
          val distanceMiles = Conversions.kilometersToMiles(distanceKilometers);
          return ((ScoredForecastResponse) new ScoredForecastResponse()
              .setDistanceKilometers(distanceKilometers)
              .setDistanceMiles(distanceMiles)
              .setUtahLocation(utahLocation));
        }).sorted(Comparator.comparingDouble(ScoredForecastResponse::getDistanceKilometers)).limit(maxResultsPerDay))
        .flatMap(scoredForecastResponse -> fetch(scoredForecastResponse.getUtahLocation()).map(forecastResponse -> ((ScoredForecastResponse) scoredForecastResponse
            .setWeatherForecast(forecastResponse.getWeatherForecast())
            .setWaterConditions(forecastResponse.getWaterConditions()))))
        .collectList()
        .map(listOfClosestDestinations -> {
          LoadingCache<LocalDate, Deque<ScoredForecastResponse>> cacheMap = Caffeine
              .newBuilder()
              .build(key -> new ConcurrentLinkedDeque<>());

          listOfClosestDestinations.forEach(scoredForecastResponse -> {
            final Map<LocalDate, List<TomorrowIOClient.Forecast>> collect = scoredForecastResponse
                .getWeatherForecast()
                .stream()
                .collect(Collectors.groupingBy(weatherForecast -> weatherForecast
                    .getStartTime()
                    .truncatedTo(ChronoUnit.DAYS)
                    .toLocalDate()));
            collect.forEach((date, forecasts) -> {
              val forecastResponse = (ScoredForecastResponse) new ScoredForecastResponse()
                  .setDistanceKilometers(scoredForecastResponse.getDistanceKilometers())
                  .setDistanceMiles(scoredForecastResponse.getDistanceMiles())
                  .setWeatherForecast(forecasts)
                  .setUtahLocation(scoredForecastResponse.getUtahLocation())
                  .setWaterConditions(scoredForecastResponse.getWaterConditions());

              forForecasts(forecasts,
                  (highLowC, highLowF) -> forecastResponse.setHighLowC(highLowC).setHighLowF(highLowF));

              cacheMap.get(date).add(forecastResponse);
            });
          });

          return cacheMap
              .asMap()
              .entrySet()
              .stream()
              .collect(Collectors.toMap(Map.Entry::getKey,
                  entry -> entry
                      .getValue()
                      .stream()
                      .map(this::score)
                      .sorted(Comparator.comparingDouble(ScoredForecastResponse::getScore).reversed())
                      .collect(Collectors.toList()),
                  (a, b) -> {throw new IllegalStateException("Two identical keys!");},
                  TreeMap::new));
        });
  }

  private ScoredForecastResponse score(ScoredForecastResponse scoredForecastResponse) {
    // Weights
    val precipitation = 100;
    val wind = 100;
    val waterTempHigh = 200;
    val weatherHigh = 100;
    val noModifiers = 100; // High = 0, Low = 1
    val distance = 175; // High = 0, Low 100

    val scorer = new Scorer<ScoredForecastResponse>(new ScoringCategory<>("precipitation",
        precipitation,
        0,
        50,
        forecastResponse -> forecastResponse
            .getWeatherForecast()
            .stream()
            .mapToDouble(TomorrowIOClient.Forecast::getPrecipitationProbability)
            .max()
            .orElseThrow(IllegalStateException::new)),
        new ScoringCategory<>("wind",
            wind,
            0,
            20,
            forecastResponse -> forecastResponse
                .getWeatherForecast()
                .stream()
                .mapToDouble(TomorrowIOClient.Forecast::getWindSpeedMph)
                .max()
                .orElseThrow(IllegalStateException::new)),
        new ScoringCategory<>("waterTempHigh",
            waterTempHigh,
            80,
            50,
            forecastResponse -> forecastResponse.getWaterConditions().getHighLowF().getHigh()),
        new ScoringCategory<>("weatherHigh",
            weatherHigh,
            90,
            50,
            forecastResponse -> forecastResponse.getWaterConditions().getHighLowF().getHigh()),
        new ScoringCategory<>("noModifiers",
            noModifiers,
            0,
            1,
            forecastResponse -> forecastResponse.getUtahLocation().getLakeModifiers().getLakeModifiers().length),
        new ScoringCategory<>("distance", distance, 0, 40, forecastResponse -> forecastResponse.getDistanceMiles()));

    return scoredForecastResponse.setScore(scorer.score(scoredForecastResponse));
  }

  @Slf4j
  @Value
  public static class Scorer<T> {
    ScoringCategory<T>[] scoringCategories;
    private final double sumOfAllWeights;

    public Scorer(ScoringCategory<T>... scoringCategories) {
      this.scoringCategories = scoringCategories;
      sumOfAllWeights = Arrays.stream(this.scoringCategories).mapToDouble(ScoringCategory::getWeight).sum();
    }

    public double score(T object) {
      val score = Arrays.stream(this.scoringCategories).mapToDouble(category -> {
        val weightedPercent = category.getWeight() / sumOfAllWeights;
        val value = category.getMapper().applyAsDouble(object);
        double interpolatedValue = linearInterpolateToPercent(category.getLow(), category.getHigh(), value);
        double partialScore = weightedPercent * interpolatedValue;

        log.info("Score({}) weight:{} * (low:{} < {} < high:{})[eval:{}] => partialScore:{}",
            category.getDescription(),
            weightedPercent,
            category.getLow(),
            value,
            category.getHigh(),
            interpolatedValue,
            partialScore);
        return partialScore;
      }).sum();

      return score;
    }
  }

  @Value
  public static class ScoringCategory<T> {
    String description;
    double weight;
    double high;
    double low;
    ToDoubleFunction<T> mapper;
  }

  @Data
  @Accessors(chain = true)
  public static class ForecastResponse {
    private UtahLocation utahLocation;
    private List<TomorrowIOClient.Forecast> weatherForecast;
    private LakeMonsterClient.WaterConditions waterConditions;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public String getName() {
      return getUtahLocation().getLocationName();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LatLong getLocation() {
      return getUtahLocation().getLocation();
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public LakeModifiersArray getLakeModifiers() {
      return getUtahLocation().getLakeModifiers();
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @ToString(callSuper = true)
  @Accessors(chain = true)
  public static class ScoredForecastResponse extends ForecastResponse {
    private double distanceKilometers;
    private double distanceMiles;
    private double score;
    private List<String> scoringDebug;
    private HighLow highLowC;
    private HighLow highLowF;
  }

}