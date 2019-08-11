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

package com.xiaomi.demoproject.EPG;

import android.content.ContentValues;
import android.text.TextUtils;

import java.util.Objects;

/**
 * A convenience class to create and insert program information entries into the database.
 */
public final class Program {
    private static final String TAG = "Program";
    private long mId;
    private String mPackageName;
    private long mChannelId;
    private String mTitle;
    private String mSeriesId;
    private long mStartTimeUtcMillis;
    private long mEndTimeUtcMillis;
    private String mDescription;

    public long getId() {
        return mId;
    }

    public void setId(long id) {
        mId = id;
    }

    public String getPackageName() {
        return mPackageName;
    }

    public void setPackageName(String packageName) {
        mPackageName = packageName;
    }

    public long getChannelId() {
        return mChannelId;
    }

    public void setChannelId(long channelId) {
        mChannelId = channelId;
    }

    public String getTitle() {
        return mTitle;
    }

    public void setTitle(String title) {
        mTitle = title;
    }

    public String getSeriesId() {
        return mSeriesId;
    }

    public void setSeriesId(String seriesId) {
        mSeriesId = seriesId;
    }

    public long getStartTimeUtcMillis() {
        return mStartTimeUtcMillis;
    }

    public void setStartTimeUtcMillis(long startTimeUtcMillis) {
        mStartTimeUtcMillis = startTimeUtcMillis;
    }

    public long getEndTimeUtcMillis() {
        return mEndTimeUtcMillis;
    }

    public void setEndTimeUtcMillis(long endTimeUtcMillis) {
        mEndTimeUtcMillis = endTimeUtcMillis;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        mDescription = description;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Program)) {
            return false;
        }
        // Compare all the properties because program ID can be invalid for the dummy programs.
        Program program = (Program) other;
        return Objects.equals(mPackageName, program.mPackageName)
                && mChannelId == program.mChannelId
                && mStartTimeUtcMillis == program.mStartTimeUtcMillis
                && mEndTimeUtcMillis == program.mEndTimeUtcMillis
                && Objects.equals(mTitle, program.mTitle)
                && Objects.equals(mSeriesId, program.mSeriesId)
                && Objects.equals(mDescription, program.mDescription);
    }

    @Override
    public String toString() {
        return "Program{" +
                "mId=" + mId +
                ", mPackageName='" + mPackageName + '\'' +
                ", mChannelId=" + mChannelId +
                ", mTitle='" + mTitle + '\'' +
                ", mSeriesId='" + mSeriesId + '\'' +
                ", mStartTimeUtcMillis=" + mStartTimeUtcMillis +
                ", mEndTimeUtcMillis=" + mEndTimeUtcMillis +
                ", mDescription='" + mDescription + '\'' +
                '}';
    }

    private static void putValue(ContentValues contentValues, String key, String value) {
        if (TextUtils.isEmpty(value)) {
            contentValues.putNull(key);
        } else {
            contentValues.put(key, value);
        }
    }

    private static void putValue(ContentValues contentValues, String key, byte[] value) {
        if (value == null || value.length == 0) {
            contentValues.putNull(key);
        } else {
            contentValues.put(key, value);
        }
    }

    @Override
    public int hashCode() {
        // Hash with all the properties because program ID can be invalid for the dummy programs.
        return Objects.hash(
                mChannelId,
                mStartTimeUtcMillis,
                mEndTimeUtcMillis,
                mTitle,
                mSeriesId,
                mDescription);
    }

}
