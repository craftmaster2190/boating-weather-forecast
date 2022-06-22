package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheService {

  public final static String REDIS_PREFIX = "BoatingWeather:";
  private final RedissonClient redissonClient;

  public static <T> List<T> toList(Iterable<T> iterable) {
    if (iterable instanceof List<?>) {
      return (List<T>) iterable;
    }
    return StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.toList());
  }

  @PostConstruct
  public void init() {
    log.info("Cache size: {} {}", redissonClient.getKeys().count(), toList(redissonClient.getKeys().getKeys()));
  }

  public <V> V get(String key) {
    return redissonClient.<V>getBucket(REDIS_PREFIX + key).get();
  }

  public <T> void put(String key, T value) {
    redissonClient.getBucket(REDIS_PREFIX + key).set(value, 12, TimeUnit.HOURS);
  }
}
