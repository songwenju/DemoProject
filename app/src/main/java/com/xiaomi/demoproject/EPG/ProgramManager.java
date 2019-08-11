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

/**
 * Manages the channels and programs for the program guide.
 */
@MainThread
public class ProgramManager {
    private static final String TAG = "ProgramManager";
    private static final boolean DEBUG = true;
    private static final String PROP_SET_EPGUPDATE_ENABLED = "persist.sys.epgupdate.isneed";

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

    private final Set<TableEntryChangedListener> mTableEntryChangedListeners = new ArraySet<>();


    private final ChannelDataManager.Listener mChannelDataManagerListener =
            new ChannelDataManager.Listener() {
                @Override
                public void onLoadFinished() {
                    updateChannels(false);
                }

                @Override
                public void onChannelListUpdated() {
                    updateChannels(false);
                }

                @Override
                public void onChannelBrowsableChanged() {
                    updateChannels(false);
                }
            };

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


    /** Update the initial time range to manage. It updates program entries. */
   public void updateInitialTimeRange(long startUtcMillis, long endUtcMillis) {
        LogUtil.i(this,"ProgramManager.updateInitialTimeRange");
        mStartUtcMillis = startUtcMillis;
        if (endUtcMillis > mEndUtcMillis) {
            mEndUtcMillis = endUtcMillis;
        }

//        mProgramDataManager.setPrefetchTimeRange(mStartUtcMillis);
        updateChannels(true);
        setTimeRange(startUtcMillis, endUtcMillis);
    }
    /**
     * Shifts the time range by the given time. Also makes ProgramGuide scroll the views.
     */
    void shiftTime(long timeMillisToScroll) {
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
     * Sets program data prefetch time range. Any program data that ends before the start time will
     * be removed from the cache later. Note that there's no limit for end time.
     *
     * <p>Prefetch should be enabled to call it.
     */
    public void setPrefetchTimeRange(long startTimeMs) {
//        SoftPreconditions.checkState(mPrefetchEnabled, TAG, "Prefetch is disabled.");
//        if (mPrefetchTimeRangeStartMs > startTimeMs) {
//            // Fetch the programs immediately to re-create the cache.
//            if (!mHandler.hasMessages(MSG_UPDATE_PREFETCH_PROGRAM)) {
//                mHandler.sendEmptyMessage(MSG_UPDATE_PREFETCH_PROGRAM);
//            }
//        }
//        mPrefetchTimeRangeStartMs = startTimeMs;
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
        int ALL = 50;
        for (int i = 0; i < ALL; i++) {
            Channel channel = new Channel("channel:" + i);
            mPrograms = new ArrayList<>();
            for (int j = 0; j < 20; j++) {
                Program program = new Program();
                program.setTitle("program:"+j);
                mPrograms.add(program);
            }

            channel.setProgramList(mPrograms);
            mChannels.add(channel);
        }
        updateTableEntriesWithoutNotification(clearPreviousTableEntries);
        // Channel update notification should be called after updating table entries, so that
        // the listener can get the entries.
        notifyChannelsUpdated();
        notifyTableEntriesUpdated();
    }

    /**
     * Updates the table entries without notifying the change.
     */
    private void updateTableEntriesWithoutNotification(boolean clear) {
        LogUtil.i(this, "ProgramManager.updateTableEntriesWithoutNotification");
        if (clear) {
            mChannelIdEntriesMap.clear();
        }

        for (Channel channel : mChannels) {
            long channelId = channel.getId();
            // Inline the updating of the mChannelIdEntriesMap here so we can only call
            // getParentalControlSettings once.
            List<TableEntry> entries = createProgramEntries(channelId, false);
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
        if (DEBUG) {
            LogUtil.d(
                    TAG,
                    "setTimeRange. {FromTime="
                            + Utils.toTimeString(fromUtcMillis)
                            + ", ToTime="
                            + Utils.toTimeString(toUtcMillis)
                            + "}");
        }
        if (mFromUtcMillis != fromUtcMillis || mToUtcMillis != toUtcMillis) {
            mFromUtcMillis = fromUtcMillis;
            mToUtcMillis = toUtcMillis;
            notifyTimeRangeUpdated();
        }
    }

    private List<TableEntry> createProgramEntries(long channelId, boolean parentalControlsEnabled) {
        LogUtil.i(this, "ProgramManager.createProgramEntries");
        List<TableEntry> entries = new ArrayList<>();
        boolean channelLocked = false;
        if (channelLocked) {
            entries.add(new TableEntry(channelId, mStartUtcMillis, Long.MAX_VALUE, true));
        } else {
            long lastProgramEndTime = mStartUtcMillis;
//            mPrograms = mProgramDataManager.getPrograms(channelId, mStartUtcMillis);
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
                long channelId, long startUtcMillis, long endUtcMillis, boolean blocked) {
            this(channelId, null, startUtcMillis, endUtcMillis, blocked);
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

    interface TableEntryChangedListener {
        void onTableEntryChanged(TableEntry entry);
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
