package com.edsapp.weather;

import android.support.annotation.Nullable;

import com.edsapp.weather.data.ForecastList;

import java.util.Map;

/**
 * Implement this interface to get update from the Data Broker.
 */
public interface WeatherListener {
    void updateData(Map<String, ForecastList> forecastMap);
    void onError(String errorText, @Nullable Throwable throwable);
}
