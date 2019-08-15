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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Handler;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.xiaomi.demoproject.EPG.ProgramManager.TableEntry;
import com.xiaomi.demoproject.LogUtil;
import com.xiaomi.demoproject.R;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.TimeUnit;

public class ProgramItemView extends TextView {
    private static final String TAG = "ProgramItemView";

    private static final long FOCUS_UPDATE_FREQUENCY = TimeUnit.SECONDS.toMillis(1);
    private static final int MAX_PROGRESS = 10000; // From android.widget.ProgressBar.MAX_VALUE

    // State indicating the focused program is the current program
    private static final int[] STATE_CURRENT_PROGRAM = {R.attr.state_current_program};

    // Workaround state in order to not use too much texture memory for RippleDrawable
    private static final int[] STATE_TOO_WIDE = {R.attr.state_program_too_wide};

    private static int sVisibleThreshold;
    private static int sItemPadding;
    private static int sCompoundDrawablePadding;
    private static TextAppearanceSpan sProgramTitleStyle;
    private static TextAppearanceSpan sGrayedOutProgramTitleStyle;
    private static TextAppearanceSpan sEpisodeTitleStyle;
    private static TextAppearanceSpan sGrayedOutEpisodeTitleStyle;

    private TableEntry mTableEntry;
    private int mMaxWidthForRipple;
    private int mTextWidth;

    // If set this flag disables requests to re-layout the parent view as a result of changing
    // this view, improving performance. This also prevents the parent view to lose child focus
    // as a result of the re-layout (see b/21378855).
    private boolean mPreventParentRelayout;

    //for TvClock
    private static Context mContext;
    private static TvClock mClock;

    private static final OnClickListener ON_CLICKED =
            new OnClickListener() {
                @Override
                public void onClick(final View view) {
                    // TODO: 2019-08-15
                }
            };

    private static final OnFocusChangeListener ON_FOCUS_CHANGED =
            new OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean hasFocus) {
                    if (hasFocus) {
                        ((ProgramItemView) view).mUpdateFocus.run();
                    } else {
                        Handler handler = view.getHandler();
                        if (handler != null) {
                            handler.removeCallbacks(((ProgramItemView) view).mUpdateFocus);
                        }
                    }
                }
            };

    private final Runnable mUpdateFocus =
            new Runnable() {
                @Override
                public void run() {
//                    refreshDrawableState();
//                    TableEntry entry = mTableEntry;
//                    if (entry == null) {
//                        // do nothing
//                        return;
//                    }
//                    if (entry.isCurrentProgram(mClock.currentTimeMillis())) {
//                        Drawable background = getBackground();
//                        if (!mProgramGuide.isActive() || mProgramGuide.isRunningAnimation()) {
//                            // If program guide is not active or is during showing/hiding,
//                            // the animation is unnecessary, skip it.
//                            background.jumpToCurrentState();
//                        }
//                        int progress =
//                                getProgress(
//                                        mClock, entry.entryStartUtcMillis, entry.entryEndUtcMillis);
//                        setProgress(background, R.id.reverse_progress, MAX_PROGRESS - progress);
//                    }
//                    if (getHandler() != null) {
//                        getHandler()
//                                .postAtTime(
//                                        this,
//                                        Utils.ceilTime(
//                                                mClock.uptimeMillis(), FOCUS_UPDATE_FREQUENCY));
//                    }
                }
            };

    public ProgramItemView(Context context) {
        this(context, null);
    }

    public ProgramItemView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ProgramItemView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setOnClickListener(ON_CLICKED);
        setOnFocusChangeListener(ON_FOCUS_CHANGED);
        mContext = context;
        mClock = new TvClock(context);

    }

    private void initIfNeeded() {
        if (sVisibleThreshold != 0) {
            return;
        }
        Resources res = getContext().getResources();

        sVisibleThreshold =
                res.getDimensionPixelOffset(R.dimen.program_guide_table_item_visible_threshold);

        sItemPadding = res.getDimensionPixelOffset(R.dimen.program_guide_table_item_padding);
        sCompoundDrawablePadding =
                res.getDimensionPixelOffset(
                        R.dimen.program_guide_table_item_compound_drawable_padding);

        ColorStateList programTitleColor =
                ColorStateList.valueOf(
                        res.getColor(
                                R.color.program_guide_table_item_program_title_text_color, null));
        ColorStateList grayedOutProgramTitleColor =
                res.getColorStateList(
                        R.color.program_guide_table_item_grayed_out_program_text_color, null);
        ColorStateList episodeTitleColor =
                ColorStateList.valueOf(
                        res.getColor(
                                R.color.program_guide_table_item_program_episode_title_text_color,
                                null));
        ColorStateList grayedOutEpisodeTitleColor =
                ColorStateList.valueOf(
                        res.getColor(
                                R.color
                                        .program_guide_table_item_grayed_out_program_episode_title_text_color,
                                null));
        int programTitleSize =
                res.getDimensionPixelSize(R.dimen.program_guide_table_item_program_title_font_size);
        int episodeTitleSize =
                res.getDimensionPixelSize(
                        R.dimen.program_guide_table_item_program_episode_title_font_size);

        sProgramTitleStyle =
                new TextAppearanceSpan(null, 0, programTitleSize, programTitleColor, null);
        sGrayedOutProgramTitleStyle =
                new TextAppearanceSpan(null, 0, programTitleSize, grayedOutProgramTitleColor, null);
        sEpisodeTitleStyle =
                new TextAppearanceSpan(null, 0, episodeTitleSize, episodeTitleColor, null);
        sGrayedOutEpisodeTitleStyle =
                new TextAppearanceSpan(null, 0, episodeTitleSize, grayedOutEpisodeTitleColor, null);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initIfNeeded();
    }

    @Override
    protected int[] onCreateDrawableState(int extraSpace) {
        if (mTableEntry != null) {
            int[] states =
                    super.onCreateDrawableState(
                            extraSpace + STATE_CURRENT_PROGRAM.length + STATE_TOO_WIDE.length);
            if (mTableEntry.isCurrentProgram(mClock.currentTimeMillis())) {
                mergeDrawableStates(states, STATE_CURRENT_PROGRAM);
            }
            if (mTableEntry.getWidth() > mMaxWidthForRipple) {
                mergeDrawableStates(states, STATE_TOO_WIDE);
            }
            return states;
        }
        return super.onCreateDrawableState(extraSpace);
    }

    public TableEntry getTableEntry() {
        return mTableEntry;
    }

    @SuppressLint("SwitchIntDef")
    public void setValues(
            TableEntry entry,
            long fromUtcMillis,
            long toUtcMillis,
            String gapTitle) {
        mTableEntry = entry;

        ViewGroup.LayoutParams layoutParams = getLayoutParams();
        if (layoutParams != null) {
            // There is no layoutParams in the tests so we skip this
            layoutParams.width = entry.getWidth();
            setLayoutParams(layoutParams);
        }
        String title = mTableEntry.program != null ? mTableEntry.program.getTitle() : null;
        LogUtil.i(this,"ProgramItemView.setValues.title:"+title);
        if (TextUtils.isEmpty(title)) {
            title = getResources().getString(R.string.program_title_for_no_information);
        }
        LogUtil.i(this,"ProgramItemView.setValues.isEntryWideEnough:"+isEntryWideEnough());
        if (!isEntryWideEnough()) {
            setText(null);
            return;
        }else {

            setText(title);
        }
        updateContentDescription(title);
        measure(MeasureSpec.UNSPECIFIED, MeasureSpec.UNSPECIFIED);
        mTextWidth = getMeasuredWidth() - getPaddingStart() - getPaddingEnd();

        // Maximum width for us to use a ripple
        mMaxWidthForRipple = GuideUtils.convertMillisToPixel(fromUtcMillis, toUtcMillis);

    }

    private boolean isEntryWideEnough() {
        LogUtil.i(this,"ProgramItemView.isEntryWideEnough.mTableEntry.getWidth:"+mTableEntry.getWidth());
        return mTableEntry != null && mTableEntry.getWidth() >= sVisibleThreshold;
    }


    private void updateContentDescription(String title) {

        // The content description includes extra information that is displayed on the detail view
        Resources resources = getResources();
        String description = title;

        Program program = mTableEntry.program;

        if (program != null) {
            String programDescription = program.getDescription();
            if (!TextUtils.isEmpty(programDescription)) {
                description += " " + programDescription;
            }
        }
        setContentDescription(description);

    }

    /**
     * Update programItemView to handle alignments of text.
     */
    public void updateVisibleArea() {
        View parentView = ((View) getParent());
        if (parentView == null) {
            return;
        }
        if (getLayoutDirection() == LAYOUT_DIRECTION_LTR) {
            layoutVisibleArea(parentView.getLeft() - getLeft(), getRight() - parentView.getRight());
        } else {
            layoutVisibleArea(getRight() - parentView.getRight(), parentView.getLeft() - getLeft());
        }
    }

    /**
     * Layout title and episode according to visible area.
     *
     * <p>Here's the spec. 1. Don't show text if it's shorter than 48dp. 2. Try showing whole text
     * in visible area by placing and wrapping text, but do not wrap text less than 30min. 3.
     * Episode title is visible only if title isn't multi-line.
     *
     * @param startOffset Offset of the start position from the enclosing view's start position.
     * @param endOffset   Offset of the end position from the enclosing view's end position.
     */
    private void layoutVisibleArea(int startOffset, int endOffset) {
        int width = mTableEntry.getWidth();
        int startPadding = Math.max(0, startOffset);
        int endPadding = Math.max(0, endOffset);
        int minWidth = Math.min(width, mTextWidth + 2 * sItemPadding);
        if (startPadding > 0 && width - startPadding < minWidth) {
            startPadding = Math.max(0, width - minWidth);
        }
        if (endPadding > 0 && width - endPadding < minWidth) {
            endPadding = Math.max(0, width - minWidth);
        }

        if (startPadding + sItemPadding != getPaddingStart()
                || endPadding + sItemPadding != getPaddingEnd()) {
            mPreventParentRelayout = true; // The size of this view is kept, no need to tell parent.
            setPaddingRelative(startPadding + sItemPadding, 0, endPadding + sItemPadding, 0);
            mPreventParentRelayout = false;
        }
    }

    public void clearValues() {
        if (getHandler() != null) {
            getHandler().removeCallbacks(mUpdateFocus);
        }

        setTag(null);
        mTableEntry = null;
    }

    private static int getStateCount(StateListDrawable stateListDrawable) {
        try {
            Object stateCount =
                    StateListDrawable.class
                            .getDeclaredMethod("getStateCount")
                            .invoke(stateListDrawable);
            return (int) stateCount;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            Log.e(TAG, "Failed to call StateListDrawable.getStateCount()", e);
            return 0;
        }
    }

    private static Drawable getStateDrawable(StateListDrawable stateListDrawable, int index) {
        try {
            Object drawable =
                    StateListDrawable.class
                            .getDeclaredMethod("getStateDrawable", Integer.TYPE)
                            .invoke(stateListDrawable, index);
            return (Drawable) drawable;
        } catch (NoSuchMethodException
                | IllegalAccessException
                | IllegalArgumentException
                | InvocationTargetException e) {
            Log.e(TAG, "Failed to call StateListDrawable.getStateDrawable(" + index + ")", e);
            return null;
        }
    }

    @Override
    public void requestLayout() {
        if (mPreventParentRelayout) {
            // Trivial layout, no need to tell parent.
            forceLayout();
        } else {
            super.requestLayout();
        }
    }
}
