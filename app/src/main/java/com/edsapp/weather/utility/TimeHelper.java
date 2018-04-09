package com.edsapp.weather.utility;

import org.joda.time.DateTime;

/**
 * Simple helper to get the list of day names forward for the amount
 */
public class TimeHelper {

    private TimeHelper() {
        throw new IllegalStateException("Utility class");
    }
    public static String[] getNextDays(int amount) {
        String[] dayList = new String[amount];
        DateTime dt = new DateTime();
        for (int i=0; i < amount; i++ ) {
            DateTime.Property pDoW = dt.dayOfWeek();
            dayList[i] = pDoW.getAsShortText();
            dt = dt.plusDays(1);
        }
        return dayList;
    }
}
