package com.craftmaster2190.boating.boatingweatherforecast;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.context.annotation.*;

@Configuration
@RequiredArgsConstructor
public class RedissonProvider {
  private final BoatingWeatherAppConfig appConfig;

  private final ObjectMapper objectMapper;

  @Bean
  public RedissonClient redissonClient() {
    val config = new Config();
    config.setCodec(new JsonJacksonCodec(objectMapper));
    config.useSingleServer().setAddress(appConfig.getRedisUrl()).setConnectionMinimumIdleSize(2);
    config.setThreads(2);
    return Redisson.create(config);
  }
}
