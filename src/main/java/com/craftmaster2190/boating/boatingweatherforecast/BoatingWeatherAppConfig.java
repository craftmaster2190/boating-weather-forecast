package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class BoatingWeatherAppConfig {
  @Value("${REDIS_URL}")
  private String redisUrl;
  @Value("${TOMORROW_IO_ACCESS_KEY}")
  private String tomorrowIoAccessKey;
}
