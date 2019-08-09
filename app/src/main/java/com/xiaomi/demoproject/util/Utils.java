/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.xiaomi.demoproject.util;

import android.text.format.DateUtils;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/** A class that includes convenience methods for accessing TvProvider database. */
public class Utils {
    private static final long RECORDING_FAILED_REASON_NONE = 0;
    private static final long HALF_MINUTE_MS = TimeUnit.SECONDS.toMillis(30);
    private static final long ONE_DAY_MS = TimeUnit.DAYS.toMillis(1);
    private static int sWidthPerHour = 0;
    /**
     * Checks if two given time (in milliseconds) are in the same day with regard to the locale
     * timezone.
     */
    public static boolean isInGivenDay(long dayToMatchInMillis, long subjectTimeInMillis) {
        TimeZone timeZone = Calendar.getInstance().getTimeZone();
        long offset = timeZone.getRawOffset();
        if (timeZone.inDaylightTime(new Date(dayToMatchInMillis))) {
            offset += timeZone.getDSTSavings();
        }
        return Utils.floorTime(dayToMatchInMillis + offset, ONE_DAY_MS)
                == Utils.floorTime(subjectTimeInMillis + offset, ONE_DAY_MS);
    }
    /**
     * Floors time to the given {@code timeUnit}. For example, if time is 5:32:11 and timeUnit is
     * one hour (60 * 60 * 1000), then the output will be 5:00:00.
     */
    public static long floorTime(long timeMs, long timeUnit) {
        return timeMs - (timeMs % timeUnit);
    }


    /** Gets the number of pixels in program guide table that corresponds to the given range. */
    public static int convertMillisToPixel(long startMillis, long endMillis) {
        // Convert to pixels first to avoid accumulation of rounding errors.
        return convertMillisToPixel(endMillis)
                - convertMillisToPixel(startMillis);
    }

    /**
     * Gets the number of pixels in program guide table that corresponds to the given milliseconds.
     */
    static int convertMillisToPixel(long millis) {
        return (int) (millis * sWidthPerHour / TimeUnit.HOURS.toMillis(1));
    }

    /**
     * Sets the width in pixels that corresponds to an hour in program guide. Assume that this is
     * called from main thread only, so, no synchronization.
     */
    static void setWidthPerHour(int widthPerHour) {
        sWidthPerHour = widthPerHour;
    }


    /** Converts time in milliseconds to a String. */
    public static String toTimeString(long timeMillis) {
        return toTimeString(timeMillis, true);
    }

    /**
     * Converts time in milliseconds to a String.
     *
     * @param fullFormat {@code true} for returning date string with a full format (e.g., Mon Aug 15
     *     20:08:35 GMT 2016). {@code false} for a short format, {e.g., 8/15/16 or 8:08 AM}, in
     *     which only the time is shown if the time is on the same day as now, and only the date is
     *     shown if it's a different day.
     */
    public static String toTimeString(long timeMillis, boolean fullFormat) {
        if (fullFormat) {
            return new Date(timeMillis).toString();
        } else {
            long currentTime = System.currentTimeMillis();
            return (String)
                    DateUtils.formatSameDayTime(
                            timeMillis,
                            System.currentTimeMillis(),
                            SimpleDateFormat.SHORT,
                            SimpleDateFormat.SHORT);
        }
    }
}
