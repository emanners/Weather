package com.edsapp.weather.network;

import android.content.Context;
import android.support.annotation.NonNull;

import com.edsapp.weather.data.WeatherResponse;

import java.io.IOException;

import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.GET;

/**
 *  Key factory class for retrofit, allows callee to create a weather service.
 */
public final class WeatherService {

    private static final String API_BASE_URL =  "http://api.openweathermap.org/";
    private static final String API_KEY = "d658a53ccc43c6548b5c77e7637c0158";
    private static final String LOCALITY = "Dublin,ie";
    private static final String MODE = "json";
    private static final String UNITS = "metric";
    private static WeatherServiceI instance;

    private static final OkHttpClient.Builder httpClient = new OkHttpClient.Builder();

    private static final Retrofit.Builder builder =
            new Retrofit.Builder()
                    .baseUrl(API_BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create());


    public interface WeatherServiceI {
        @GET("/data/2.5/forecast") Call<WeatherResponse> getForecast();
    }

    private static <S> S createWeatherService(Class<S> serviceClass, Context context) {
            httpClient.addInterceptor(new Interceptor() {
              @Override
              public Response intercept(@NonNull Chain chain) throws IOException {
                  Request original = chain.request();
                  HttpUrl originalHttpUrl = original.url();

                  HttpUrl url = originalHttpUrl.newBuilder()
                          .addQueryParameter("APPID", API_KEY)
                          .addQueryParameter("q", LOCALITY)
                          .addQueryParameter("mode", MODE)
                          .addQueryParameter("units", UNITS)
                          .build();

                  Request.Builder requestBuilder = original.newBuilder()
                          .url(url);

                  Request request = requestBuilder.build();
                  return chain.proceed(request);
              }
            });

            if (context != null) // For testing only
            httpClient.addInterceptor(new FakeInterceptor(context));

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            httpClient.addInterceptor(logging);

            OkHttpClient client = httpClient.build();
            Retrofit retrofit = builder.client(client).build();
            return retrofit.create(serviceClass);
    }

    public static WeatherServiceI getInstance() {
        if (instance == null)
            instance = WeatherService.createWeatherService(WeatherServiceI.class, null);
        return instance;
    }


}