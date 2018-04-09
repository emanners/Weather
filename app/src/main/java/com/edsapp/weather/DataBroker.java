package com.edsapp.weather;

import android.content.Context;
import android.content.SharedPreferences;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.edsapp.weather.data.ForecastList;
import com.edsapp.weather.data.WeatherResponse;
import com.edsapp.weather.network.WeatherService;

import com.google.gson.Gson;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;


import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Class that looks after caching logic, and data retrieval, and update to Listener
 */
public class DataBroker {

    // Set of Time Periods for configuration
    public static final long TWO_HOURS_MS = 1000L*60L*60L *2L;
    public static final long IMMEDIATE = 10000L;

    private static final String TAG = DataBroker.class.getSimpleName();
    private final SharedPreferences sharedPreferences;
    private long timePeriod;

    private final WeatherListener weatherListener;
    private final Context context;
    private final Gson gson = new Gson();

    /**
     * @param context The context
     * @param weatherListener will get updates and errors as appropriate
     */
    public DataBroker(@NonNull Context context, @NonNull final WeatherListener weatherListener) {
        this.context = context;
        this.weatherListener = weatherListener;
        this.sharedPreferences = context.getSharedPreferences("com.edsapp.weather", Context.MODE_PRIVATE );
        this.timePeriod = TWO_HOURS_MS; // default
    }


    public void setTimePeriod(long timePeriod) { // TODO param should be setup as Type

        this.timePeriod = timePeriod;
    }

    public  long getTimePeriod() {
        return this.timePeriod;
    }

    private void updateCache(@NonNull String jsonString) throws IOException {
        try (FileOutputStream stream = context.openFileOutput("response.json", Context.MODE_PRIVATE)) {
            stream.write(jsonString.getBytes());
        }
        setExpiryNow();
    }

    private boolean isExpired() {
        Date now = new Date();
        long lastCheckMS = sharedPreferences.getLong("lastCheckMS", 0L);
        return (now.getTime() - lastCheckMS) > timePeriod;
    }

    private void setExpiryNow() {
        Date now = new Date();
        sharedPreferences.edit().putLong("lastCheckMS", now.getTime()).apply();
    }

    public void getForecast() {
        if (isExpired()) {
            getLiveForecast();
        } else {
            getCachedForecast();
        }
    }

    private void onEvent(@NonNull Map<String, ForecastList> forecastMap) {
        if (weatherListener != null ) {
            weatherListener.updateData(forecastMap);
        }
    }

    private void onEvent(@NonNull String string, @Nullable Throwable t) {
        Log.e(TAG, t != null ? t.getMessage() : "");
        if (weatherListener != null ) {
            weatherListener.onError(string, t);
        }
    }

    private void getCachedForecast() {
        try {
            FileInputStream json = context.openFileInput("response.json");
            WeatherResponse weatherResponse = gson.fromJson(new InputStreamReader(json), WeatherResponse.class);

            if (weatherResponse != null) {
                Map<String, ForecastList> forecastMap =  getStringForecastListHashMap(weatherResponse);
                onEvent(forecastMap);
            } else {
                Log.e(TAG, "Error in response");
                onEvent(context.getString(R.string.comms_error), null);
            }

        } catch (com.google.gson.JsonSyntaxException | IOException t) { // Bad read of data.
            onEvent(context.getString(R.string.comms_error), t);
        }
    }

    public void getLiveForecast() {

        Call<WeatherResponse> forecastCall = WeatherService.getInstance().getForecast();

        forecastCall.enqueue(new Callback<WeatherResponse>() {
            @Override
            public void onResponse(@NonNull Call<WeatherResponse> call,
                                   @NonNull Response<WeatherResponse> response ) {
                if (response.isSuccessful()) {
                    Map<String, ForecastList> forecastMap =  getStringForecastListHashMap(response.body());
                    if (forecastMap != null) {
                        onEvent(forecastMap);
                    } else {
                        onEvent(context.getString(R.string.format_error), null);
                    }
                    try {
                        updateCache(new Gson().toJson(response.body())); // TODO more efficient way get plain JSON Mwould be to do it an interceptor level.
                    } catch (IOException e) {
                        onEvent(context.getString(R.string.cache_error), null);
                    }
                } else {
                    onEvent(context.getString(R.string.comms_error), null);
                }
            }

            @Override
            public void onFailure(@NonNull Call<WeatherResponse> call, @NonNull Throwable t) {
                // something went completely south (like no internet connection)
                Log.e(TAG, t.getMessage(), t);
                onEvent(context.getString(R.string.comms_error), t);
            }
        });
    }

    private Map<String, ForecastList> getStringForecastListHashMap(@Nullable WeatherResponse result) {

        if (result == null)
            return null;

        HashMap<String, ForecastList> candidateList = new HashMap<>();
        DateTimeZone.setDefault(DateTimeZone.UTC);
        DateTime dt =  new DateTime();
        DateTime.Property currentDay = dt.dayOfWeek();

        for (ForecastList dayForecast: result.getForecastList()) {

            long utc = dayForecast.getDt().longValue() * 1000L;
            DateTime newDt = new DateTime(utc);
            DateTime.Property dow = newDt.dayOfWeek();

            if (dow.equals(currentDay) && newDt.getHourOfDay() >= 12) { // assuming its in chronological order, the midday gets priority.
                candidateList.put(dow.getAsShortText(), dayForecast);
                dt = dt.plusDays(1);
                currentDay = dt.dayOfWeek();
            }
        }
        return candidateList;
    }
}
