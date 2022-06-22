package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.Data;
import org.springframework.boot.context.properties.*;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "boating-weather")
@EnableConfigurationProperties
public class BoatingWeatherAppConfig {
  private String redisHost;
  private String redisPort;
  private String tomorrowIoAccessKey;
}
