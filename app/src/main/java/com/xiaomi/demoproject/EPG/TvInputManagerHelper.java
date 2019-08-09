package com.xiaomi.demoproject.EPG;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvInputManager.TvInputCallback;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.xiaomi.demoproject.util.CommonUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class TvInputManagerHelper {
    private static final String TAG = "TvInputManagerHelper";
    private static final boolean DEBUG = false;

    public interface TvInputManagerInterface {
        TvInputInfo getTvInputInfo(String inputId);

        Integer getInputState(String inputId);

        void registerCallback(TvInputCallback internalCallback, Handler handler);

        void unregisterCallback(TvInputCallback internalCallback);

        List<TvInputInfo> getTvInputList();

    }

    private static final class TvInputManagerImpl implements TvInputManagerInterface {
        private final TvInputManager delegate;

        private TvInputManagerImpl(TvInputManager delegate) {
            this.delegate = delegate;
        }

        @Override
        public TvInputInfo getTvInputInfo(String inputId) {
            return delegate.getTvInputInfo(inputId);
        }

        @Override
        public Integer getInputState(String inputId) {
            return delegate.getInputState(inputId);
        }

        @Override
        public void registerCallback(TvInputCallback internalCallback, Handler handler) {
            delegate.registerCallback(internalCallback, handler);
        }

        @Override
        public void unregisterCallback(TvInputCallback internalCallback) {
            delegate.unregisterCallback(internalCallback);
        }

        @Override
        public List<TvInputInfo> getTvInputList() {
            return delegate.getTvInputList();
        }

    }

    /** Types of HDMI device and bundled tuner. */
    public static final int TYPE_CEC_DEVICE = -2;

    public static final int TYPE_BUNDLED_TUNER = -3;
    public static final int TYPE_CEC_DEVICE_RECORDER = -4;
    public static final int TYPE_CEC_DEVICE_PLAYBACK = -5;
    public static final int TYPE_MHL_MOBILE = -6;

    private static final String PERMISSION_ACCESS_ALL_EPG_DATA =
            "com.android.providers.tv.permission.ACCESS_ALL_EPG_DATA";
    private static final String[] mPhysicalTunerBlackList = {
    };
    private static final String META_LABEL_SORT_KEY = "input_sort_key";

    /** The default tv input priority to show. */
    private static final ArrayList<Integer> DEFAULT_TV_INPUT_PRIORITY = new ArrayList<>();

    static {
        DEFAULT_TV_INPUT_PRIORITY.add(TYPE_BUNDLED_TUNER);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_TUNER);
        DEFAULT_TV_INPUT_PRIORITY.add(TYPE_CEC_DEVICE);
        DEFAULT_TV_INPUT_PRIORITY.add(TYPE_CEC_DEVICE_RECORDER);
        DEFAULT_TV_INPUT_PRIORITY.add(TYPE_CEC_DEVICE_PLAYBACK);
        DEFAULT_TV_INPUT_PRIORITY.add(TYPE_MHL_MOBILE);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_HDMI);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_DVI);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_COMPONENT);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_SVIDEO);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_COMPOSITE);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_DISPLAY_PORT);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_VGA);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_SCART);
        DEFAULT_TV_INPUT_PRIORITY.add(TvInputInfo.TYPE_OTHER);
    }

    private static final String[] PARTNER_TUNER_INPUT_PREFIX_BLACKLIST = {
    };

    private static final String[] TESTABLE_INPUTS = {
        "com.android.tv.testinput/.TestTvInputService"
    };

    private final Context mContext;
    private final PackageManager mPackageManager;
    protected final TvInputManagerInterface mTvInputManager;
    private final Map<String, Integer> mInputStateMap = new HashMap<>();
    private final Map<String, TvInputInfo> mInputMap = new HashMap<>();
    private final Map<String, String> mTvInputLabels = new ArrayMap<>();
    private final Map<String, String> mTvInputCustomLabels = new ArrayMap<>();
    private final Map<String, Boolean> mInputIdToPartnerInputMap = new HashMap<>();

    private final Map<String, CharSequence> mTvInputApplicationLabels = new ArrayMap<>();
    private final Map<String, Drawable> mTvInputApplicationIcons = new ArrayMap<>();
    private final Map<String, Drawable> mTvInputAppliactionBanners = new ArrayMap<>();

    private final TvInputCallback mInternalCallback =
            new TvInputCallback() {
                @Override
                public void onInputStateChanged(String inputId, int state) {
                    if (DEBUG) Log.d(TAG, "onInputStateChanged " + inputId + " state=" + state);
                    if (isInBlackList(inputId)) {
                        return;
                    }
                    mInputStateMap.put(inputId, state);
                    for (TvInputCallback callback : mCallbacks) {
                        callback.onInputStateChanged(inputId, state);
                    }
                }

                @Override
                public void onInputAdded(String inputId) {
                    if (DEBUG) Log.d(TAG, "onInputAdded " + inputId);
                    if (isInBlackList(inputId)) {
                        return;
                    }
                    TvInputInfo info = mTvInputManager.getTvInputInfo(inputId);
                    if (info != null) {
                        mInputMap.put(inputId, info);
                        CharSequence label = info.loadLabel(mContext);
                        // in tests the label may be missing just use the input id
                        mTvInputLabels.put(inputId, label != null ? label.toString() : inputId);
                        CharSequence inputCustomLabel = info.loadCustomLabel(mContext);
                        if (inputCustomLabel != null) {
                            mTvInputCustomLabels.put(inputId, inputCustomLabel.toString());
                        }
                        mInputStateMap.put(inputId, mTvInputManager.getInputState(inputId));
                        mInputIdToPartnerInputMap.put(inputId, isPartnerInput(info));
                    }
                    for (TvInputCallback callback : mCallbacks) {
                        callback.onInputAdded(inputId);
                    }
                }

                @Override
                public void onInputRemoved(String inputId) {
                    if (DEBUG) Log.d(TAG, "onInputRemoved " + inputId);
                    mInputMap.remove(inputId);
                    mTvInputLabels.remove(inputId);
                    mTvInputCustomLabels.remove(inputId);
                    mTvInputApplicationLabels.remove(inputId);
                    mTvInputApplicationIcons.remove(inputId);
                    mTvInputAppliactionBanners.remove(inputId);
                    mInputStateMap.remove(inputId);
                    mInputIdToPartnerInputMap.remove(inputId);
                    for (TvInputCallback callback : mCallbacks) {
                        callback.onInputRemoved(inputId);
                    }
                }

                @Override
                public void onInputUpdated(String inputId) {
                    if (DEBUG) Log.d(TAG, "onInputUpdated " + inputId);
                    if (isInBlackList(inputId)) {
                        return;
                    }
                    TvInputInfo info = mTvInputManager.getTvInputInfo(inputId);
                    mInputMap.put(inputId, info);
                    mTvInputLabels.put(inputId, info.loadLabel(mContext).toString());
                    CharSequence inputCustomLabel = info.loadCustomLabel(mContext);
                    if (inputCustomLabel != null) {
                        mTvInputCustomLabels.put(inputId, inputCustomLabel.toString());
                    }
                    mTvInputApplicationLabels.remove(inputId);
                    mTvInputApplicationIcons.remove(inputId);
                    mTvInputAppliactionBanners.remove(inputId);
                    for (TvInputCallback callback : mCallbacks) {
                        callback.onInputUpdated(inputId);
                    }

                }

                @Override
                public void onTvInputInfoUpdated(TvInputInfo inputInfo) {
                    if (DEBUG) Log.d(TAG, "onTvInputInfoUpdated " + inputInfo);
                    mInputMap.put(inputInfo.getId(), inputInfo);
                    mTvInputLabels.put(inputInfo.getId(), inputInfo.loadLabel(mContext).toString());
                    CharSequence inputCustomLabel = inputInfo.loadCustomLabel(mContext);
                    if (inputCustomLabel != null) {
                        mTvInputCustomLabels.put(inputInfo.getId(), inputCustomLabel.toString());
                    }
                    for (TvInputCallback callback : mCallbacks) {
                        callback.onTvInputInfoUpdated(inputInfo);
                    }

                }
            };

    private final Handler mHandler = new Handler();
    private boolean mStarted;
    private final HashSet<TvInputCallback> mCallbacks = new HashSet<>();
    private final Comparator<TvInputInfo> mTvInputInfoComparator;

    public TvInputManagerHelper(Context context) {
        this(context, createTvInputManagerWrapper(context));
    }

    @Nullable
    protected static TvInputManagerImpl createTvInputManagerWrapper(Context context) {
        TvInputManager tvInputManager =
                (TvInputManager) context.getSystemService(Context.TV_INPUT_SERVICE);
        return tvInputManager == null ? null : new TvInputManagerImpl(tvInputManager);
    }

    @VisibleForTesting
    protected TvInputManagerHelper(
            Context context, @Nullable TvInputManagerInterface tvInputManager) {
        mContext = context.getApplicationContext();
        mPackageManager = context.getPackageManager();
        mTvInputManager = tvInputManager;
        mTvInputInfoComparator = new InputComparatorInternal(this);
    }

    public void start() {
        if (!hasTvInputManager()) {
            // Not a TV device
            return;
        }
        if (mStarted) {
            return;
        }
        if (DEBUG) Log.d(TAG, "start");
        mStarted = true;
        mTvInputManager.registerCallback(mInternalCallback, mHandler);
        mInputMap.clear();
        mTvInputLabels.clear();
        mTvInputCustomLabels.clear();
        mTvInputApplicationLabels.clear();
        mTvInputApplicationIcons.clear();
        mTvInputAppliactionBanners.clear();
        mInputStateMap.clear();
        mInputIdToPartnerInputMap.clear();
        for (TvInputInfo input : mTvInputManager.getTvInputList()) {
            if (DEBUG) Log.d(TAG, "Input detected " + input);
            String inputId = input.getId();
            if (isInBlackList(inputId)) {
                continue;
            }
            mInputMap.put(inputId, input);
            int state = mTvInputManager.getInputState(inputId);
            mInputStateMap.put(inputId, state);
            mInputIdToPartnerInputMap.put(inputId, isPartnerInput(input));
        }

    }

    public void stop() {
        if (!mStarted) {
            return;
        }
        mTvInputManager.unregisterCallback(mInternalCallback);
        mStarted = false;
        mInputStateMap.clear();
        mInputMap.clear();
        mTvInputLabels.clear();
        mTvInputCustomLabels.clear();
        mTvInputApplicationLabels.clear();
        mTvInputApplicationIcons.clear();
        mTvInputAppliactionBanners.clear();
        ;
        mInputIdToPartnerInputMap.clear();
    }


    /**
     * Checks if the input is from a partner.
     *
     * <p>It's visible for comparator test. Package private is enough for this method, but public is
     * necessary to workaround mockito bug.
     */
    @VisibleForTesting
    public boolean isPartnerInput(TvInputInfo inputInfo) {
        return isSystemInput(inputInfo) && !isBundledInput(inputInfo);
    }

    /** Does the input have {@link ApplicationInfo#FLAG_SYSTEM} set. */
    public boolean isSystemInput(TvInputInfo inputInfo) {
        return inputInfo != null
                && (inputInfo.getServiceInfo().applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM)
                        != 0;
    }

    /** Is the input one known bundled inputs not written by OEM/SOCs. */
    public boolean isBundledInput(TvInputInfo inputInfo) {
        return inputInfo != null
                && CommonUtils.isInBundledPackageSet(
                        inputInfo.getServiceInfo().applicationInfo.packageName);
    }

    /**
     * Returns if the given input is bundled and written by OEM/SOCs. This returns the cached
     * result.
     */
    public boolean isPartnerInput(String inputId) {
        Boolean isPartnerInput = mInputIdToPartnerInputMap.get(inputId);
        return (isPartnerInput != null) ? isPartnerInput : false;
    }

    /**
     * Is (Context.TV_INPUT_SERVICE) available.
     *
     * <p>This is only available on TV devices.
     */
    public boolean hasTvInputManager() {
        return mTvInputManager != null;
    }

    /** Loads label of {@code info}. */
    public String loadLabel(TvInputInfo info) {
        String label = mTvInputLabels.get(info.getId());
        if (label == null) {
            label = info.loadLabel(mContext).toString();
            mTvInputLabels.put(info.getId(), label);
        }
        return label;
    }

    /** Loads custom label of {@code info} */
    public String loadCustomLabel(TvInputInfo info) {
        String customLabel = mTvInputCustomLabels.get(info.getId());
        if (customLabel == null) {
            CharSequence customLabelCharSequence = info.loadCustomLabel(mContext);
            if (customLabelCharSequence != null) {
                customLabel = customLabelCharSequence.toString();
                mTvInputCustomLabels.put(info.getId(), customLabel);
            }
        }
        return customLabel;
    }

    /** Gets the tv input application's label. */
    public CharSequence getTvInputApplicationLabel(CharSequence inputId) {
        return mTvInputApplicationLabels.get(inputId);
    }

    /** Stores the tv input application's label. */
    public void setTvInputApplicationLabel(String inputId, CharSequence label) {
        mTvInputApplicationLabels.put(inputId, label);
    }

    /** Gets the tv input application's icon. */
    public Drawable getTvInputApplicationIcon(String inputId) {
        return mTvInputApplicationIcons.get(inputId);
    }

    /** Stores the tv input application's icon. */
    public void setTvInputApplicationIcon(String inputId, Drawable icon) {
        mTvInputApplicationIcons.put(inputId, icon);
    }

    /** Gets the tv input application's banner. */
    public Drawable getTvInputApplicationBanner(String inputId) {
        return mTvInputAppliactionBanners.get(inputId);
    }

    public TvInputInfo getTvInputInfo(String inputId) {
        if (!mStarted) {
            return null;
        }
        if (inputId == null) {
            return null;
        }
        return mInputMap.get(inputId);
    }

    /**
     * Returns TvInputInfo's input state.
     *
     * @param inputInfo
     * @return An Integer which stands for the input state {@link
     */
    public int getInputState(@Nullable TvInputInfo inputInfo) {
        return inputInfo == null
                ? TvInputManager.INPUT_STATE_DISCONNECTED
                : getInputState(inputInfo.getId());
    }

    public int getInputState(String inputId) {
        if (!mStarted) {
            return TvInputManager.INPUT_STATE_DISCONNECTED;
        }
        Integer state = mInputStateMap.get(inputId);
        if (state == null) {
            Log.w(TAG, "getInputState: no such input (id=" + inputId + ")");
            return TvInputManager.INPUT_STATE_DISCONNECTED;
        }
        return state;
    }

    public void addCallback(TvInputCallback callback) {
        mCallbacks.add(callback);
    }

    public void removeCallback(TvInputCallback callback) {
        mCallbacks.remove(callback);
    }

    private int getInputSortKey(TvInputInfo input) {
        return input.getServiceInfo().metaData.getInt(META_LABEL_SORT_KEY, Integer.MAX_VALUE);
    }

    private boolean isInputPhysicalTuner(TvInputInfo input) {
        String packageName = input.getServiceInfo().packageName;
        if (Arrays.asList(mPhysicalTunerBlackList).contains(packageName)) {
            return false;
        }

        if (input.createSetupIntent() == null) {
            return false;
        } else {
            boolean mayBeTunerInput =
                    mPackageManager.checkPermission(
                                    PERMISSION_ACCESS_ALL_EPG_DATA,
                                    input.getServiceInfo().packageName)
                            == PackageManager.PERMISSION_GRANTED;
            if (!mayBeTunerInput) {
                try {
                    ApplicationInfo ai =
                            mPackageManager.getApplicationInfo(
                                    input.getServiceInfo().packageName, 0);
                    if ((ai.flags
                                    & (ApplicationInfo.FLAG_SYSTEM
                                            | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))
                            == 0) {
                        return false;
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isInBlackList(String inputId) {

        if (CommonUtils.isRoboTest()) return false;
        if (CommonUtils.isRunningInTest()) {
            for (String testableInput : TESTABLE_INPUTS) {
                if (testableInput.equals(inputId)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Default comparator for TvInputInfo.
     *
     * <p>It's static class that accepts {@link TvInputManagerHelper} as parameter to test. To test
     * comparator, we need to mock API in parent class such as {@link #isPartnerInput}, but it's
     * impossible for an inner class to use mocked methods. (i.e. Mockito's spy doesn't work)
     */
    @VisibleForTesting
    static class InputComparatorInternal implements Comparator<TvInputInfo> {
        private final TvInputManagerHelper mInputManager;

        public InputComparatorInternal(TvInputManagerHelper inputManager) {
            mInputManager = inputManager;
        }

        @Override
        public int compare(TvInputInfo lhs, TvInputInfo rhs) {
            if (mInputManager.isPartnerInput(lhs) != mInputManager.isPartnerInput(rhs)) {
                return mInputManager.isPartnerInput(lhs) ? -1 : 1;
            }
            return mInputManager.loadLabel(lhs).compareTo(mInputManager.loadLabel(rhs));
        }
    }

}
