package com.craftmaster2190.boating.boatingweatherforecast;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.reactive.function.client.*;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class WebClientProvider {
  @Bean
  public WebClient webClient(WebClient.Builder webClientBuilder) {
    return webClientBuilder
        .exchangeStrategies(ExchangeStrategies
            .builder()
            .codecs(codecs -> codecs
                .defaultCodecs()
                .maxInMemorySize(Math.toIntExact(DataSize.ofMegabytes(100).toBytes())))
            .build())
        .clientConnector(new ReactorClientHttpConnector(HttpClient.create(ConnectionProvider
            .builder("myConnectionProvider")
            .maxConnections(1)
            .pendingAcquireTimeout(Duration.ofDays(1))
            .pendingAcquireMaxCount(Integer.MAX_VALUE)
            .build())))
        .build();
  }
}
