
package com.xiaomi.demoproject.EPG;

import android.content.ContentResolver;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.tv.TvContract.Channels;
import android.media.tv.TvInputManager.TvInputCallback;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.util.ArraySet;
import android.util.Log;
import android.util.MutableInt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * The class to manage channel data. Basic features: reading channel list and each channel's current
 * program, and updating the values of {@link Channels#COLUMN_BROWSABLE}, {@link
 * Channels#COLUMN_LOCKED}. This class is not thread-safe and under an assumption that its public
 * methods are called in only the main thread.
 */
@AnyThread
public class ChannelDataManager {
    private static final String TAG = "ChannelDataManager";
    private static final boolean DEBUG = false;

    private static final int MSG_UPDATE_CHANNELS = 1000;

    private final Context mContext;
    private final TvInputManagerHelper mInputManager;
    private boolean mStarted;
    private boolean mDbLoadFinished;
    private final List<Runnable> mPostRunnablesAfterChannelUpdate = new ArrayList<>();

    private final Set<Listener> mListeners = new CopyOnWriteArraySet<>();
    // Use container class to support multi-thread safety. This value can be set only on the main
    // thread.
    private volatile UnmodifiableChannelData mData = new UnmodifiableChannelData();

    private final Set<Long> mBrowsableUpdateChannelIds = new HashSet<>();
    private final Set<Long> mLockedUpdateChannelIds = new HashSet<>();

    private final ContentResolver mContentResolver;

    private final TvInputCallback mTvInputCallback =
            new TvInputCallback() {
                @Override
                public void onInputAdded(String inputId) {

                }

                @Override
                public void onInputRemoved(String inputId) {

                }
            };

    @MainThread
    public ChannelDataManager(Context context, TvInputManagerHelper inputManager) {
        this(
                context,
                inputManager,
                context.getContentResolver());
    }

    @MainThread
    @VisibleForTesting
    ChannelDataManager(
            Context context,
            TvInputManagerHelper inputManager,
            ContentResolver contentResolver) {
        mContext = context;
        mInputManager = inputManager;
        mContentResolver = contentResolver;
        // Detect duplicate channels while sorting.

    }


    /**
     * Starts the manager. If data is ready, {@link Listener#onLoadFinished()} will be called.
     */
    @MainThread
    public void start() {

    }

    /**
     * Adds a {@link Listener}.
     */
    public void addListener(Listener listener) {
        if (DEBUG) Log.d(TAG, "addListener " + listener);
        if (listener != null) {
            mListeners.add(listener);
        }
    }

    /**
     * Removes a {@link Listener}.
     */
    public void removeListener(Listener listener) {
        if (DEBUG) Log.d(TAG, "removeListener " + listener);
        if (listener != null) {
            mListeners.remove(listener);
        }
    }

    /**
     * Adds a {@link ChannelListener} for a specific channel with the channel ID {@code channelId}.
     */
    public void addChannelListener(Long channelId, ChannelListener listener) {
        ChannelWrapper channelWrapper = mData.channelWrapperMap.get(channelId);
        if (channelWrapper == null) {
            return;
        }
        channelWrapper.addListener(listener);
    }

    /**
     * Removes a {@link ChannelListener} for a specific channel with the channel ID {@code
     * channelId}.
     */
    public void removeChannelListener(Long channelId, ChannelListener listener) {
        ChannelWrapper channelWrapper = mData.channelWrapperMap.get(channelId);
        if (channelWrapper == null) {
            return;
        }
        channelWrapper.removeListener(listener);
    }

    /**
     * Checks whether data is ready.
     */
    public boolean isDbLoadFinished() {
        return mDbLoadFinished;
    }

    /**
     * Returns the number of channels.
     */
    public int getChannelCount() {
        return mData.channels.size();
    }

    /**
     * Returns a list of channels.
     */
    public List<Channel> getChannelList() {
        return new ArrayList<>(mData.channels);
    }

    /**
     * Returns a list of browsable channels.
     */
    public List<Channel> getBrowsableChannelList() {
        List<Channel> channels = new ArrayList<>();
        for (Channel channel : mData.channels) {
            channels.add(channel);
        }
        return channels;
    }

    /**
     * Returns the total channel count for a given input.
     *
     * @param inputId The ID of the input.
     */
    public int getChannelCountForInput(String inputId) {
        MutableInt count = mData.channelCountMap.get(inputId);
        return count == null ? 0 : count.value;
    }

    /**
     * Checks if the channel exists in DB.
     *
     * <p>Note that the channels of the removed inputs can not be obtained from {@link #getChannel}.
     * In that case this method is used to check if the channel exists in the DB.
     */
    public boolean doesChannelExistInDb(long channelId) {
        return mData.channelWrapperMap.get(channelId) != null;
    }

    /**
     * Gets the channel with the channel ID {@code channelId}.
     */
    public Channel getChannel(Long channelId) {
        ChannelWrapper channelWrapper = mData.channelWrapperMap.get(channelId);
        if (channelWrapper == null) {
            return null;
        }
        return channelWrapper.mChannel;
    }



    public void notifyChannelBrowsableChanged() {
        for (Listener l : mListeners) {
            l.onChannelBrowsableChanged();
        }
    }

    private void notifyChannelListUpdated() {
        for (Listener l : mListeners) {
            l.onChannelListUpdated();
        }
    }

    private void notifyLoadFinished() {
        for (Listener l : mListeners) {
            l.onLoadFinished();
        }
    }


    /**
     * The value change will be applied to DB when applyPendingDbOperation is called.
     */
    public void updateLocked(Long channelId, boolean locked) {
        ChannelWrapper channelWrapper = mData.channelWrapperMap.get(channelId);
        if (channelWrapper == null) {
            return;
        }
        if (channelWrapper.mChannel.isLocked() != locked) {
            channelWrapper.mChannel.setLocked(locked);
            if (locked == channelWrapper.mLockedInDb) {
                mLockedUpdateChannelIds.remove(channelWrapper.mChannel.getId());
            } else {
                mLockedUpdateChannelIds.add(channelWrapper.mChannel.getId());
            }
            channelWrapper.notifyChannelUpdated();
        }
    }

    @MainThread
    private void addChannel(ChannelData data, Channel channel) {
        //[DroidLogic]
        //When add channels, filter the channels according to current type.
        if (DEBUG) Log.d(TAG, "===== addChannel channel=" + channel);

    }

    @MainThread
    private void clearChannels() {
        mData = new UnmodifiableChannelData();
    }


    /**
     * A listener for ChannelDataManager. The callbacks are called on the main thread.
     */
    public interface Listener {
        /**
         * Called when data load is finished.
         */
        void onLoadFinished();

        /**
         * Called when channels are added, deleted, or updated. But, when browsable is changed, it
         * won't be called. Instead, {@link #onChannelBrowsableChanged} will be called.
         */
        void onChannelListUpdated();

        /**
         * Called when browsable of channels are changed.
         */
        void onChannelBrowsableChanged();
    }

    /**
     * A listener for individual channel change. The callbacks are called on the main thread.
     */
    public interface ChannelListener {
        /**
         * Called when the channel has been removed in DB.
         */
        void onChannelRemoved(Channel channel);

        /**
         * Called when values of the channel has been changed.
         */
        void onChannelUpdated(Channel channel);
    }

    private class ChannelWrapper {
        final Set<ChannelListener> mChannelListeners = new ArraySet<>();
        final Channel mChannel;
        boolean mLockedInDb;

        ChannelWrapper(Channel channel) {
            mChannel = channel;
            mLockedInDb = channel.isLocked();
        }

        void addListener(ChannelListener listener) {
            mChannelListeners.add(listener);
        }

        void removeListener(ChannelListener listener) {
            mChannelListeners.remove(listener);
        }

        void notifyChannelUpdated() {
            for (ChannelListener l : mChannelListeners) {
                l.onChannelUpdated(mChannel);
            }
        }

        void notifyChannelRemoved() {
            for (ChannelListener l : mChannelListeners) {
                l.onChannelRemoved(mChannel);
            }
        }
    }

    /**
     * Container class which includes channel data that needs to be synced. This class is modifiable
     * and used for changing channel data. e.g. TvInputCallback, or AsyncDbTask.onPostExecute.
     */
    @MainThread
    private static class ChannelData {
        final Map<Long, ChannelWrapper> channelWrapperMap;
        final Map<String, MutableInt> channelCountMap;
        final List<Channel> channels;

        ChannelData() {
            channelWrapperMap = new HashMap<>();
            channelCountMap = new HashMap<>();
            channels = new ArrayList<>();
        }

        ChannelData(ChannelData data) {
            channelWrapperMap = new HashMap<>(data.channelWrapperMap);
            channelCountMap = new HashMap<>(data.channelCountMap);
            channels = new ArrayList<>(data.channels);
        }

        ChannelData(
                Map<Long, ChannelWrapper> channelWrapperMap,
                Map<String, MutableInt> channelCountMap,
                List<Channel> channels) {
            this.channelWrapperMap = channelWrapperMap;
            this.channelCountMap = channelCountMap;
            this.channels = channels;
        }
    }

    /**
     * Unmodifiable channel data.
     */
    @MainThread
    private static class UnmodifiableChannelData extends ChannelData {
        UnmodifiableChannelData() {
            super(
                    Collections.unmodifiableMap(new HashMap<>()),
                    Collections.unmodifiableMap(new HashMap<>()),
                    Collections.unmodifiableList(new ArrayList<>()));
        }

        UnmodifiableChannelData(ChannelData data) {
            super(
                    Collections.unmodifiableMap(data.channelWrapperMap),
                    Collections.unmodifiableMap(data.channelCountMap),
                    Collections.unmodifiableList(data.channels));
        }
    }
}
