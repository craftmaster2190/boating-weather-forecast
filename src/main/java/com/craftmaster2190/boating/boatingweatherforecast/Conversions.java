package com.craftmaster2190.boating.boatingweatherforecast;

public class Conversions {
  public static double distanceInKilometers(LatLong latLong1, LatLong latLong2) {
    return distanceInKilometers(latLong1.getLatitude(),
        latLong2.getLatitude(),
        latLong1.getLongitude(),
        latLong2.getLongitude());
  }

  public static double distanceInKilometers(double latitude1, double latitude2, double longitude1, double longitude2) {

    // The math module contains a function
    // named toRadians which converts from
    // degrees to radians.
    longitude1 = Math.toRadians(longitude1);
    longitude2 = Math.toRadians(longitude2);
    latitude1 = Math.toRadians(latitude1);
    latitude2 = Math.toRadians(latitude2);

    // Haversine formula
    double dlon = longitude2 - longitude1;
    double dlat = latitude2 - latitude1;
    double a = Math.pow(Math.sin(dlat / 2),
        2) + Math.cos(latitude1) * Math.cos(latitude2) * Math.pow(Math.sin(dlon / 2), 2);

    double c = 2 * Math.asin(Math.sqrt(a));

    // Radius of earth in kilometers. Use 3956
    // for miles
    double r = 6371;

    // calculate the result
    return (c * r);
  }

  public static double kilometersToMiles(double distanceKilometers) {
    return distanceKilometers * 0.621371;
  }

  public static double kph2mph(double kph) {
    return kph * 0.62137119223733;
  }

  public static double celsius2fahrenheit(double celsius) {
    return celsius * 1.8 + 32;
  }

  public static double fahrenheit2celsius(double fahrenheit) {
    return (fahrenheit - 32) * 0.5556;
  }
}
