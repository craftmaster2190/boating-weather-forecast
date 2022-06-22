package com.craftmaster2190.boating.boatingweatherforecast;

import org.springframework.lang.*;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.*;

public class StreamUtils {

  private StreamUtils() {
    throw new UnsupportedOperationException("Do not instantiate");
  }

  /**
   * Creates a stream from the collection or an empty stream if null or empty.
   *
   * @implNote null-safe
   */
  @NonNull
  public static <T> Stream<T> stream(@Nullable Collection<T> collection) {
    if (CollectionUtils.isEmpty(collection)) {
      return Stream.empty();
    }
    return collection.stream();
  }

  /**
   * Creates a stream from the iterator or an empty stream if null or empty.
   *
   * @implNote null-safe
   */
  @NonNull
  public static <T> Stream<T> stream(@Nullable Iterator<T> iterator) {
    if (iterator == null) {
      return Stream.empty();
    }
    return StreamSupport.stream(Spliterators.spliteratorUnknownSize(iterator, 0), false);
  }
}
