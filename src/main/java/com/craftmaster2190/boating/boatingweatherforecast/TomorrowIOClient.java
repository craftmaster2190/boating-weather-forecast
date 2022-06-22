package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class TomorrowIOClient {
  public static final String TOMORROW_API_URI = "https://api.tomorrow.io/v4/timelines";
  public static final ZoneId AMERICA_DENVER = ZoneId.of("America/Denver");
  private final static String REDIS_PREFIX = "TomorrowIO:";
  private final WebClient webClient;
  private final BoatingWeatherAppConfig appConfig;
  private final CacheService cacheService;

  public Mono<List<Forecast>> getForecast(UtahLocation utahLocation, Timestep timestep) {
    val cacheKey = REDIS_PREFIX + utahLocation.name() + timestep.name();
    {
      List<Forecast> cachedValue = cacheService.get(cacheKey);
      if (cachedValue != null) {
        return Mono.just(cachedValue);
      }
    }

    val start = Instant.now();
    log.info("START getForecast({}, {})", utahLocation, timestep);
    final LatLong location = utahLocation.getLocation();
    return webClient
        .get()
        .uri(String.format(
            "%s?location=%s,%s&fields=temperature,windSpeed,precipitationProbability&timesteps=%s&units=metric&apikey=%s",
            TOMORROW_API_URI,
            location.getLatitude(),
            location.getLongitude(),
            timestep.getCode(),
            appConfig.getTomorrowIoAccessKey()))
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .retrieve()
        .bodyToMono(TomorrowIOResponse.class)
        .retryWhen(Retry.backoff(3, Duration.ofSeconds(3)))
        .map(tomorrowIOResponse -> transform(tomorrowIOResponse, timestep))
        .doOnSuccess(value -> {
          cacheService.put(cacheKey, value);
          log.info("DONE getForecast({}, {}) {}", utahLocation, timestep, Duration.between(start, Instant.now()));
        });
  }

  public List<Forecast> transform(TomorrowIOResponse tomorrowIOResponse, Timestep timestep) {
    return tomorrowIOResponse
        .getData()
        .getTimelines()
        .stream()
        .flatMap(timeline -> timeline.getIntervals().stream())
        .filter(interval -> timestep != Timestep.EVERY_HOUR || isBetweenBoatingHours(interval))
        .map(interval -> {
          val values = interval.getValues();
          return new Forecast()
              .setStartTime(interval.getStartTime().atZone(AMERICA_DENVER))
              .setPrecipitationProbability(values.getPrecipitationProbability())
              .setTemperatureC(values.getTemperature())
              .setTemperatureF(Conversions.celsius2fahrenheit(values.getTemperature()))
              .setWindSpeedKph(values.getWindSpeed())
              .setWindSpeedMph(Conversions.kph2mph(values.getWindSpeed()));
        })
        .collect(Collectors.toList());
  }

  private boolean isBetweenBoatingHours(TomorrowIOResponse.TomorrowIOData.Timeline.Interval interval) {
    val hour = interval.getStartTime().atZone(AMERICA_DENVER).getHour();
    return (hour >= 8 && hour <= 20);
  }

  @Getter
  @RequiredArgsConstructor
  public enum Timestep {
    EVERY_HOUR("1h"), EVERY_DAY("1d");

    private final String code;
  }

  @Data
  public static class TomorrowIOResponse {
    private TomorrowIOData data;

    @Data
    public static class TomorrowIOData {
      private List<Timeline> timelines;

      @Data
      public static class Timeline {
        private String timestep;
        private Instant startTime;
        private Instant endTime;
        private List<Interval> intervals;

        @Data
        public static class Interval {
          private Instant startTime;
          private Values values;

          @Data
          public static class Values {
            private double precipitationProbability;
            private double temperature;
            private double windSpeed;
          }
        }
      }
    }
  }

  @Data
  @Accessors(chain = true)
  public static class Forecast {
    private ZonedDateTime startTime;
    private double precipitationProbability;
    private double temperatureC;
    private double temperatureF;
    private double windSpeedKph;
    private double windSpeedMph;
  }
}

