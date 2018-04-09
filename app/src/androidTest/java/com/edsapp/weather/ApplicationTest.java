package com.edsapp.weather;

import com.edsapp.weather.data.WeatherResponse;
import com.edsapp.weather.network.WeatherService;
import com.edsapp.weather.utility.TimeHelper;


import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import retrofit2.Call;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 * <a href="http://d.android.com/tools/testing/testing_android.html">Testing Fundamentals</a>
 */
public class ApplicationTest {


    @Test
    public void testForecastService() throws Exception {

        Call<WeatherResponse> forecastCall = WeatherService.getInstance().getForecast();
        WeatherResponse result = forecastCall.execute().body();
        assertNotNull(result);
    }


    public void testJodaDays () {

        String[] strings = TimeHelper.getNextDays(5);

        assertTrue(strings.length == 5);
    }

    public void testEquivalenceTime() {

        DateTimeZone.setDefault(DateTimeZone.UTC);

        DateTime dt = new DateTime();

        DateTime.Property pDoW = dt.dayOfWeek();
        //String dayString = pDoW.getAsShortText();

        long utc = 1473422400L * 1000L;

        DateTime newDt = new DateTime(utc);
        DateTime.Property pDoW2 = newDt.dayOfWeek();
        String newDayString = pDoW2.getAsShortText();

        assertTrue(pDoW2.equals(pDoW));

    }
}