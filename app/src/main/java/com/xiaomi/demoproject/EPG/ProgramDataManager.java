
package com.xiaomi.demoproject.EPG;

import android.content.ContentResolver;
import android.content.Context;
import android.media.tv.TvContract.Programs;
import android.support.annotation.AnyThread;
import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;
import android.util.ArraySet;
import android.util.LruCache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@MainThread
public class ProgramDataManager {
    private static final String TAG = "ProgramDataManager";
    private static final boolean DEBUG = false;
    private static final String PROP_SET_UPDATE_IMMEDIATE_ENABLED = "persist.sys.update.immediate";

    // To prevent from too many program update operations at the same time, we give random interval
    // between PERIODIC_PROGRAM_UPDATE_MIN_MS and PERIODIC_PROGRAM_UPDATE_MAX_MS.
    @VisibleForTesting
    static final long PERIODIC_PROGRAM_UPDATE_MIN_MS = TimeUnit.MINUTES.toMillis(5);

    private static final long PERIODIC_PROGRAM_UPDATE_MAX_MS = TimeUnit.MINUTES.toMillis(10);
    private static final long PROGRAM_PREFETCH_UPDATE_WAIT_MS = TimeUnit.SECONDS.toMillis(5);
    // TODO: need to optimize consecutive DB updates.
    private static final long CURRENT_PROGRAM_UPDATE_WAIT_MS = TimeUnit.SECONDS.toMillis(5);
    @VisibleForTesting static final long PROGRAM_GUIDE_SNAP_TIME_MS = TimeUnit.MINUTES.toMillis(30);

    // TODO: Use TvContract constants, once they become public.
    private static final String PARAM_START_TIME = "start_time";
    private static final String PARAM_END_TIME = "end_time";
    // COLUMN_CHANNEL_ID, COLUMN_END_TIME_UTC_MILLIS are added to detect duplicated programs.
    // Duplicated programs are always consecutive by the sorting order.
    private static final String SORT_BY_TIME =
            Programs.COLUMN_START_TIME_UTC_MILLIS
                    + ", "
                    + Programs.COLUMN_CHANNEL_ID
                    + ", "
                    + Programs.COLUMN_END_TIME_UTC_MILLIS;

    private static final int MSG_UPDATE_CURRENT_PROGRAMS = 1000;
    private static final int MSG_UPDATE_ONE_CURRENT_PROGRAM = 1001;
    private static final int MSG_UPDATE_PREFETCH_PROGRAM = 1002;
    private static final int MSG_UPDATE_CURRENT_PLAYING_PROGRAM = 1003;

    private final ContentResolver mContentResolver;
    private boolean mStarted;
    // Updated only on the main thread.
    private volatile boolean mCurrentProgramsLoadFinished;


    private final Map<Long, Program> mChannelIdCurrentProgramMap = new ConcurrentHashMap<>();

    private final Set<Listener> mListeners = new ArraySet<>();


    private boolean mPrefetchEnabled;
    private long mProgramPrefetchUpdateWaitMs;
    private long mLastPrefetchTaskRunMs;
    private Map<Long, ArrayList<Program>> mChannelIdProgramCache = new HashMap<>();

    // Any program that ends prior to this time will be removed from the cache
    // when a channel's current program is updated.
    // Note that there's no limit for end time.
    private long mPrefetchTimeRangeStartMs;

    private boolean mPauseProgramUpdate = false;
    private final LruCache<Long, Program> mZeroLengthProgramCache = new LruCache<>(10);

    // [DroidLogic]
    private Context mContext = null;
    private TvClock mClock;

    @MainThread
    public ProgramDataManager(Context context) {
        this(context.getContentResolver(), new TvClock(context));

        mContext = context;
    }

    @VisibleForTesting
    ProgramDataManager(
            ContentResolver contentResolver,
            TvClock time) {
        mClock = time;
        mContentResolver = contentResolver;
    }


    /**
     * Set the program prefetch update wait which gives the delay to query all programs from DB to
     * prevent from too frequent DB queries. Default value is {@link
     * #PROGRAM_PREFETCH_UPDATE_WAIT_MS}
     */
    @VisibleForTesting
    void setProgramPrefetchUpdateWait(long programPrefetchUpdateWaitMs) {
        mProgramPrefetchUpdateWaitMs = programPrefetchUpdateWaitMs;
    }

    /** Starts the manager. */
    public void start() {
        if (mStarted) {
            return;
        }
        mStarted = true;
        // Should be called directly instead of posting MSG_UPDATE_CURRENT_PROGRAMS message
    }

    /**
     * Stops the manager. It clears manager states and runs pending DB operations. Added listeners
     * aren't automatically removed by this method.
     */
    @VisibleForTesting
    public void stop() {
        if (!mStarted) {
            return;
        }
        mStarted = false;
    }

    @AnyThread
    public boolean isCurrentProgramsLoadFinished() {
        return mCurrentProgramsLoadFinished;
    }

    /** Returns the current program at the specified channel. */
    @AnyThread
    public Program getCurrentProgram(long channelId) {
        return mChannelIdCurrentProgramMap.get(channelId);
    }

    /** Returns all the current programs. */
    @AnyThread
    public List<Program> getCurrentPrograms() {
        return new ArrayList<>(mChannelIdCurrentProgramMap.values());
    }

    public List<Program> getPrograms(long channelId, long startUtcMillis) {

        return null;
    }

    /** A listener interface to receive notification on program data retrieval from DB. */
    public interface Listener {
        /**
         * Called when a Program data is now available through getProgram() after the DB operation
         * is done which wasn't before. This would be called only if fetched data is around the
         * selected program.
         */
        void onProgramUpdated();
    }

    /** Adds the {@link Listener}. */
    public void addListener(Listener listener) {
        mListeners.add(listener);
    }

    /** Removes the {@link Listener}. */
    public void removeListener(Listener listener) {
        mListeners.remove(listener);
    }



}
