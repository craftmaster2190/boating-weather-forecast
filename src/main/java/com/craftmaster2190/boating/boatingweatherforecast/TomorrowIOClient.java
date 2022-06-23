package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.*;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.*;
import java.util.*;
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

  public Mono<List<Forecast>> getForecast(UtahLocation utahLocation) {
    val cacheKey = REDIS_PREFIX + utahLocation.name();
    {
      List<Forecast> cachedValue = cacheService.get(cacheKey);
      if (cachedValue != null) {
        return Mono.just(cachedValue);
      }
    }

    val start = Instant.now();
    log.info("START getForecast({})", utahLocation);
    final LatLong location = utahLocation.getLocation();
    return webClient
        .get()
        .uri(String.format(
            "%s?location=%s,%s&fields=temperature,windSpeed,precipitationProbability&timesteps=1h,1d&units=metric&apikey=%s",
            TOMORROW_API_URI,
            location.getLatitude(),
            location.getLongitude(),
            appConfig.getTomorrowIoAccessKey()))
        .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .exchangeToMono(clientResponse -> {
          if (!clientResponse.statusCode().isError()) {
            return clientResponse.bodyToMono(TomorrowIOResponse.class);
          }
          return clientResponse
              .toBodilessEntity()
              .flatMap(result -> Optional
                  .ofNullable(result.getHeaders().getFirst(HttpHeaders.RETRY_AFTER))
                  .map(Integer::parseUnsignedInt)
                  .filter(retrySeconds -> retrySeconds < 30)
                  .map(retrySeconds -> {
                    val sleepDelay = Duration.ofSeconds(retrySeconds);
                    log.info("getForecast SLEEPING {} to make TomorrowIO API rate limiting happy.", sleepDelay);
                    return Mono
                        .<TomorrowIOResponse>error(WebClientResponseException.create(clientResponse.rawStatusCode(),
                            clientResponse.statusCode().getReasonPhrase(),
                            result.getHeaders(),
                            null,
                            null))
                        .delayElement(sleepDelay);
                  })
                  .orElseGet(Mono::empty));
        })
        .retryWhen(Retry.max(1))
        .onErrorResume(throwable -> {
          if (throwable instanceof WebClientResponseException) {
            val webClientResponseException = (WebClientResponseException) throwable;
            log.error("Error Response {} Headers: {} {}",
                webClientResponseException.getStatusCode(),
                webClientResponseException.getHeaders(),
                webClientResponseException.getResponseBodyAsString());
          }
          return Mono.empty();
        })
        .map(this::transform)
        .doOnSuccess(value -> {
          cacheService.put(cacheKey, value);
          log.info("DONE getForecast({}) {}", utahLocation, Duration.between(start, Instant.now()));
        })
        .flatMap(value -> {
          val duration = Duration.between(start, Instant.now());
          val oneSecond = Duration.ofSeconds(1);
          if (duration.compareTo(oneSecond) < 0) {
            final Duration sleepDelay = oneSecond.minus(duration);
            log.info("getForecast SLEEPING {} to make TomorrowIO API rate limiting happy.", sleepDelay);
            return Mono.just(value).delayElement(sleepDelay); // Add delay to make TomorrowIO RateLimiting happy
          }
          return Mono.just(value);
        });
  }

  public List<Forecast> transform(TomorrowIOResponse tomorrowIOResponse) {
    return tomorrowIOResponse
        .getData()
        .getTimelines()
        .stream()
        .flatMap(timeline -> timeline.getIntervals().stream())
        .filter(this::isBetweenBoatingHours)
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

