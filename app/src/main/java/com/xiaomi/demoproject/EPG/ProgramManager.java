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

import android.support.annotation.MainThread;
import android.util.ArraySet;

import com.xiaomi.demoproject.LogUtil;
import com.xiaomi.demoproject.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.xiaomi.demoproject.EPG.EPGActivity.ALL;

/**
 * Manages the channels and programs for the program guide.
 */
@MainThread
public class ProgramManager {
    private static final String TAG = "ProgramManager";
    private static final boolean DEBUG = true;

    /**
     * If the first entry's visible duration is shorter than this value, we clip the entry out.
     * Note: If this value is larger than 1 min, it could cause mismatches between the entry's
     * position and detailed view's time range.
     */
    public static final long FIRST_ENTRY_MIN_DURATION = TimeUnit.MINUTES.toMillis(1);

    private static final long INVALID_ID = -1;

    private long mStartUtcMillis;
    private long mEndUtcMillis;
    private long mFromUtcMillis;
    private long mToUtcMillis;

    private List<Channel> mChannels = new ArrayList<>();
    private final Map<Long, List<TableEntry>> mChannelIdEntriesMap = new HashMap<>();


    private final Set<Listener> mListeners = new ArraySet<>();
    private final Set<TableEntriesUpdatedListener> mTableEntriesUpdatedListeners = new ArraySet<>();


    private List<Program> mPrograms;


    public ProgramManager() {
    }


    /**
     * Adds a {@link Listener}.
     */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /**
     * Removes a {@link Listener}.
     */
    void removeListener(Listener listener) {
        mListeners.remove(listener);
    }


    /**
     * Update the initial time range to manage. It updates program entries.
     */
    public void updateInitialTimeRange(long startUtcMillis, long endUtcMillis) {
        LogUtil.i(this, "ProgramManager.updateInitialTimeRange");
        mStartUtcMillis = startUtcMillis;
        if (endUtcMillis > mEndUtcMillis) {
            mEndUtcMillis = endUtcMillis;
        }

        updateChannels(true);
        setTimeRange(startUtcMillis, endUtcMillis);
    }

    /**
     * Shifts the time range by the given time. Also makes ProgramGuide scroll the views.
     */
    void shiftTime(long timeMillisToScroll) {
        LogUtil.i(this,"ProgramManager.shiftTime.timeMillisToScroll:"+timeMillisToScroll);
        long fromUtcMillis = mFromUtcMillis + timeMillisToScroll;
        long toUtcMillis = mToUtcMillis + timeMillisToScroll;
        if (fromUtcMillis < mStartUtcMillis) {
            fromUtcMillis = mStartUtcMillis;
            toUtcMillis += mStartUtcMillis - fromUtcMillis;
        }
        if (toUtcMillis > mEndUtcMillis) {
            fromUtcMillis -= toUtcMillis - mEndUtcMillis;
            toUtcMillis = mEndUtcMillis;
        }
        setTimeRange(fromUtcMillis, toUtcMillis);
    }


    /**
     * Returned the scrolled(shifted) time in milliseconds.
     */
    public long getShiftedTime() {
        return mFromUtcMillis - mStartUtcMillis;
    }

    /**
     * Returns the start time set by {@link #updateInitialTimeRange}.
     */
    long getStartTime() {
        return mStartUtcMillis;
    }

    /**
     * Returns the program index of the program at {@code time} or -1 if not found.
     */
    int getProgramIndexAtTime(long channelId, long time) {
        List<TableEntry> entries = mChannelIdEntriesMap.get(channelId);
        for (int i = 0; i < entries.size(); ++i) {
            TableEntry entry = entries.get(i);
            if (entry.entryStartUtcMillis <= time && time < entry.entryEndUtcMillis) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Returns the start time of currently managed time range, in UTC millisecond.
     */
    long getFromUtcMillis() {
        return mFromUtcMillis;
    }

    /**
     * Returns the end time of currently managed time range, in UTC millisecond.
     */
    long getToUtcMillis() {
        return mToUtcMillis;
    }

    /**
     * Returns the number of the currently managed channels.
     */
    int getChannelCount() {
        return mChannels.size();
    }

    /**
     * Returns a {@link Channel} at a given {@code channelIndex} of the currently managed channels.
     * Returns {@code null} if such a channel is not found.
     */
    Channel getChannel(int channelIndex) {
        if (channelIndex < 0 || channelIndex >= getChannelCount()) {
            return null;
        }
        return mChannels.get(channelIndex);
    }

    /**
     * Returns the number of "entries", which lies within the currently managed time range, for a
     * given {@code channelId}.
     */
    int getTableEntryCount(long channelId) {
        return mChannelIdEntriesMap.get(channelId).size();
    }

    /**
     * Returns an entry as {@link Program} for a given {@code channelId} and {@code index} of
     * entries within the currently managed time range. Returned {@link Program} can be a dummy one
     * (e.g., whose channelId is INVALID_ID), when it corresponds to a gap between programs.
     */
    TableEntry getTableEntry(long channelId, int index) {
        return mChannelIdEntriesMap.get(channelId).get(index);
    }


    private void updateChannels(boolean clearPreviousTableEntries) {
        if (DEBUG) LogUtil.d(TAG, "updateChannels");

        for (int i = 0; i < ALL; i++) {

            Channel channel = new Channel("channel:" + i);

            channel.setProgramList(mPrograms);
            mChannels.add(channel);
        }
        getTabEntries(clearPreviousTableEntries);
        // Channel update notification should be called after updating table entries, so that
        // the listener can get the entries.
        notifyChannelsUpdated();
        notifyTableEntriesUpdated();
    }

    /**
     * Updates the table entries without notifying the change.
     */
    private void getTabEntries(boolean clear) {
        LogUtil.i(this, "ProgramManager.getTabEntries");
        if (clear) {
            mChannelIdEntriesMap.clear();
        }

        for (Channel channel : mChannels) {
            long channelId = channel.getId();
            // Inline the updating of the mChannelIdEntriesMap here so we can only call
            // getParentalControlSettings once.
            List<TableEntry> entries = createProgramEntries(channelId, false);
            LogUtil.i(this, "ProgramManager.getTabEntries.entries:" + entries);
            mChannelIdEntriesMap.put(channelId, entries);

            int size = entries.size();
            if (DEBUG) {
                LogUtil.d(
                        TAG,
                        "Programs are loaded for channel "
                                + channel.getId()
                                + ", loaded size = "
                                + size);
            }
            if (size == 0) {
                continue;
            }
            TableEntry lastEntry = entries.get(size - 1);
            if (mEndUtcMillis < lastEntry.entryEndUtcMillis
                    && lastEntry.entryEndUtcMillis != Long.MAX_VALUE) {
                mEndUtcMillis = lastEntry.entryEndUtcMillis;
            }
        }
        if (mEndUtcMillis > mStartUtcMillis) {
            for (Channel channel : mChannels) {
                long channelId = channel.getId();
                List<TableEntry> entries = mChannelIdEntriesMap.get(channelId);
                if (entries.isEmpty()) {
                    entries.add(new TableEntry(channelId, mStartUtcMillis, mEndUtcMillis));
                } else {
                    TableEntry lastEntry = entries.get(entries.size() - 1);
                    if (mEndUtcMillis > lastEntry.entryEndUtcMillis) {
                        entries.add(
                                new TableEntry(
                                        channelId, lastEntry.entryEndUtcMillis, mEndUtcMillis));
                    } else if (lastEntry.entryEndUtcMillis == Long.MAX_VALUE) {
                        entries.remove(entries.size() - 1);
                        entries.add(
                                new TableEntry(
                                        lastEntry.channelId,
                                        lastEntry.program,
                                        lastEntry.entryStartUtcMillis,
                                        mEndUtcMillis,
                                        lastEntry.mIsBlocked));
                    }
                }
            }
        }
    }


    public List<Channel> getChannels() {
        return mChannels;
    }

    private void setTimeRange(long fromUtcMillis, long toUtcMillis) {
        LogUtil.i(this,"ProgramManager.setTimeRange.fromUtcMillis:"+fromUtcMillis+",toUtcMillis:"+toUtcMillis);
        if (mFromUtcMillis != fromUtcMillis || mToUtcMillis != toUtcMillis) {
            mFromUtcMillis = fromUtcMillis;
            mToUtcMillis = toUtcMillis;
            notifyTimeRangeUpdated();
        }
    }

    private List<TableEntry> createProgramEntries(long channelId, boolean parentalControlsEnabled) {
        LogUtil.i(this, "ProgramManager.createProgramEntries");
        List<TableEntry> entries = new ArrayList<>();

        long lastProgramEndTime = mStartUtcMillis;

        mPrograms = new ArrayList<>();

        Program program0 = new Program();
        program0.setTitle("Google Play Movies ");
        program0.setStartTimeUtcMillis(1561401000000L);
        program0.setEndTimeUtcMillis(1561402800000L);

        Program program1 = new Program();
        program1.setTitle("Google Play Movies ");
        program1.setStartTimeUtcMillis(1561402800000L);
        program1.setEndTimeUtcMillis(1561404600000L);

        Program program2 = new Program();
        program2.setTitle("Google Play Movies ");
        program2.setStartTimeUtcMillis(1561404600000L);
        program2.setEndTimeUtcMillis(1561406400000L);

        Program program3 = new Program();
        program3.setTitle("Google Play Movies ");
        program3.setStartTimeUtcMillis(1561406400000L);
        program3.setEndTimeUtcMillis(1561408200000L);


        Program program4 = new Program();
        program4.setTitle("Google Play Movies ");
        program4.setStartTimeUtcMillis(1561408200000L);
        program4.setEndTimeUtcMillis(1561410000000L);

        Program program5 = new Program();
        program5.setTitle("Google Play Movies ");
        program5.setStartTimeUtcMillis(1561410000000L);
        program5.setEndTimeUtcMillis(1561411800000L);

        Program program6 = new Program();
        program6.setTitle("Google Play Movies ");
        program6.setStartTimeUtcMillis(1561411800000L);
        program6.setEndTimeUtcMillis(1561413600000L);

        Program program7 = new Program();
        program7.setTitle("Google Play Movies ");
        program7.setStartTimeUtcMillis(1561413600000L);
        program7.setEndTimeUtcMillis(1561415400000L);
        mPrograms.add(program0);
        mPrograms.add(program1);
        mPrograms.add(program2);
        mPrograms.add(program3);
        mPrograms.add(program4);
        mPrograms.add(program5);
        mPrograms.add(program6);
        mPrograms.add(program7);
        LogUtil.i(this,"ProgramManager.createProgramEntries.mStartUtcMillis:"+mStartUtcMillis);
        for (Program program : mPrograms) {
            if (program.getChannelId() == INVALID_ID) {
                // Dummy program.
                continue;
            }
            long programStartTime = Math.max(program.getStartTimeUtcMillis(), mStartUtcMillis);
            long programEndTime = program.getEndTimeUtcMillis();
            if (programStartTime > lastProgramEndTime) {
                // Gap since the last program.
                entries.add(new TableEntry(channelId, lastProgramEndTime, programStartTime));
                lastProgramEndTime = programStartTime;
            }
            if (programEndTime > lastProgramEndTime) {
                entries.add(
                        new TableEntry(
                                channelId,
                                program,
                                lastProgramEndTime,
                                programEndTime,
                                false));
                lastProgramEndTime = programEndTime;
            }
        }


        if (entries.size() > 1) {
            TableEntry secondEntry = entries.get(1);
            if (secondEntry.entryStartUtcMillis < mStartUtcMillis + FIRST_ENTRY_MIN_DURATION) {
                // If the first entry's width doesn't have enough width, it is not good to show
                // the first entry from UI perspective. So we clip it out.
                entries.remove(0);
                entries.set(
                        0,
                        new TableEntry(
                                secondEntry.channelId,
                                secondEntry.program,
                                mStartUtcMillis,
                                secondEntry.entryEndUtcMillis,
                                secondEntry.mIsBlocked));
            }
        }
        return entries;
    }


    private void notifyChannelsUpdated() {
        LogUtil.i(this, "ProgramManager.notifyChannelsUpdated");
        for (Listener listener : mListeners) {
            listener.onChannelsUpdated();
        }
    }

    private void notifyTimeRangeUpdated() {
        for (Listener listener : mListeners) {
            listener.onTimeRangeUpdated();
        }
    }

    private void notifyTableEntriesUpdated() {
        for (TableEntriesUpdatedListener listener : mTableEntriesUpdatedListeners) {
            listener.onTableEntriesUpdated();
        }
    }

    /**
     * Entry for program guide table. An "entry" can be either an actual program or a gap between
     * android.support.v17.leanback.widget.HorizontalGridView} ignores margins between items.
     */
    static class TableEntry {
        /**
         * Channel ID which this entry is included.
         */
        final long channelId;

        /**
         * Program corresponding to the entry. {@code null} means that this entry is a gap.
         */
        final Program program;


        /**
         * Start time of entry in UTC milliseconds.
         */
        final long entryStartUtcMillis;

        /**
         * End time of entry in UTC milliseconds
         */
        final long entryEndUtcMillis;

        private final boolean mIsBlocked;

        private TableEntry(long channelId, long startUtcMillis, long endUtcMillis) {
            this(channelId, null, startUtcMillis, endUtcMillis, false);
        }


        private TableEntry(
                long channelId,
                Program program,
                long entryStartUtcMillis,
                long entryEndUtcMillis,
                boolean isBlocked) {
            this.channelId = channelId;
            this.program = program;
            this.entryStartUtcMillis = entryStartUtcMillis;
            this.entryEndUtcMillis = entryEndUtcMillis;
            mIsBlocked = isBlocked;
        }

        /**
         * A stable id useful for {@link android.support.v7.widget.RecyclerView.Adapter}.
         */
        long getId() {
            // using a negative entryEndUtcMillis keeps it from conflicting with program Id
            return program != null ? program.getId() : -entryEndUtcMillis;
        }


        /**
         * Returns true if this channel is blocked.
         */
        boolean isBlocked() {
            return mIsBlocked;
        }

        /**
         * Returns true if this program is on the air.
         */
        boolean isCurrentProgram(long currentTime) {
            long current = currentTime;
            return entryStartUtcMillis <= current && entryEndUtcMillis > current;
        }


        /**
         * Returns the width of table entry, in pixels.
         */
        int getWidth() {

            return GuideUtils.convertMillisToPixel(entryStartUtcMillis, entryEndUtcMillis);
        }

        @Override
        public String toString() {
            return "TableEntry{"
                    + "hashCode="
                    + hashCode()
                    + ", channelId="
                    + channelId
                    + ", program="
                    + program
                    + ", startTime="
                    + Utils.toTimeString(entryStartUtcMillis)
                    + ", endTimeTime="
                    + Utils.toTimeString(entryEndUtcMillis)
                    + "}";
        }
    }

    interface Listener {

        void onChannelsUpdated();

        void onTimeRangeUpdated();
    }

    interface TableEntriesUpdatedListener {
        void onTableEntriesUpdated();
    }

    public static class ListenerAdapter implements Listener {

        @Override
        public void onChannelsUpdated() {
        }

        @Override
        public void onTimeRangeUpdated() {
        }
    }
}
