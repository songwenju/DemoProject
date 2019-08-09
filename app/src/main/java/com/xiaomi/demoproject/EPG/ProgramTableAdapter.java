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

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.os.Handler;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.RecycledViewPool;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.TextAppearanceSpan;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityManager.AccessibilityStateChangeListener;
import android.widget.ImageView;
import android.widget.TextView;

import com.xiaomi.demoproject.LogUtil;
import com.xiaomi.demoproject.R;
import com.xiaomi.demoproject.util.CommonUtils;
import com.xiaomi.demoproject.util.Utils;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProgramTableAdapter extends RecyclerView.Adapter<ProgramTableAdapter.ProgramRowViewHolder>
        implements ProgramManager.TableEntryChangedListener {
    private static final String TAG = "ProgramTableAdapter";
    private static final boolean DEBUG = true;

    private final Context mContext;
    private final ProgramManager mProgramManager;
    private final AccessibilityManager mAccessibilityManager;
    //    private final ProgramGuide mProgramGuide;
    private final Handler mHandler = new Handler();
    private final List<ProgramListAdapter> mProgramListAdapters = new ArrayList<>();
    private final RecycledViewPool mRecycledViewPool;
    // views to be be reused when displaying critic scores
    private final String mProgramTitleForNoInformation;
    private final String mProgramTitleForBlockedChannel;
    private final int mChannelTextColor;
    private final int mAnimationDuration;
    private final int mDetailPadding;
    private final TextAppearanceSpan mEpisodeTitleStyle;


    public ProgramTableAdapter(Context context, ProgramManager programManager) {
        LogUtil.i(this, "ProgramTableAdapter.ProgramTableAdapter");
        mContext = context;
        mAccessibilityManager =
                (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);

//        mProgramGuide = programGuide;
        mProgramManager = programManager;

        Resources res = context.getResources();
        mProgramTitleForNoInformation = res.getString(R.string.program_title_for_no_information);
        mProgramTitleForBlockedChannel = res.getString(R.string.program_title_for_blocked_channel);
        mChannelTextColor =
                res.getColor(
                        R.color.program_guide_table_header_column_channel_number_text_color, null);
        mAnimationDuration =
                res.getInteger(R.integer.program_guide_table_detail_fade_anim_duration);
        mDetailPadding = res.getDimensionPixelOffset(R.dimen.program_guide_table_detail_padding);

        int episodeTitleSize =
                res.getDimensionPixelSize(
                        R.dimen.program_guide_table_detail_episode_title_text_size);
        ColorStateList episodeTitleColor =
                ColorStateList.valueOf(
                        res.getColor(
                                R.color.program_guide_table_detail_episode_title_text_color, null));
        mEpisodeTitleStyle =
                new TextAppearanceSpan(null, 0, episodeTitleSize, episodeTitleColor, null);

        mRecycledViewPool = new RecycledViewPool();
        mRecycledViewPool.setMaxRecycledViews(
                R.layout.program_guide_table_item,
                context.getResources().getInteger(R.integer.max_recycled_view_pool_epg_table_item));
        mProgramManager.addListener(
                new ProgramManager.ListenerAdapter() {
                    @Override
                    public void onChannelsUpdated() {
                        update();
                    }
                });
        update();
        mProgramManager.addTableEntryChangedListener(this);
    }

    private void update() {
        mProgramListAdapters.clear();
        for (int i = 0; i < mProgramManager.getChannelCount(); i++) {
            ProgramListAdapter listAdapter =
                    new ProgramListAdapter(mContext.getResources(), mProgramManager, i);
            mProgramManager.addTableEntriesUpdatedListener(listAdapter);
            mProgramListAdapters.add(listAdapter);
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return mProgramListAdapters.size();
    }

    @Override
    public int getItemViewType(int position) {
        return R.layout.program_guide_table_row;
    }

    @Override
    public void onBindViewHolder(ProgramRowViewHolder holder, int position) {
        holder.onBind(position);
    }

    @Override
    public void onBindViewHolder(ProgramRowViewHolder holder, int position, List<Object> payloads) {
        if (!payloads.isEmpty()) {
            holder.updateDetailView();
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public ProgramRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        ProgramRow programRow = (ProgramRow) itemView.findViewById(R.id.row);
        programRow.setRecycledViewPool(mRecycledViewPool);
        return new ProgramRowViewHolder(itemView);
    }

    @Override
    public void onTableEntryChanged(ProgramManager.TableEntry entry) {

    }


    class ProgramRowViewHolder extends RecyclerView.ViewHolder
            implements ProgramRow.ChildFocusListener {

        private final ViewGroup mContainer;
        private final ProgramRow mProgramRow;
        private ProgramManager.TableEntry mSelectedEntry;
        private Animator mDetailOutAnimator;
        private Animator mDetailInAnimator;
        private final Runnable mDetailInStarter =
                new Runnable() {
                    @Override
                    public void run() {
                        mProgramRow.removeOnScrollListener(mOnScrollListener);
                        if (mDetailInAnimator != null) {
                            mDetailInAnimator.start();
                        }
                    }
                };
        private final Runnable mUpdateDetailViewRunnable =
                new Runnable() {
                    @Override
                    public void run() {
                        updateDetailView();
                    }
                };

        private final RecyclerView.OnScrollListener mOnScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        onHorizontalScrolled();
                    }
                };

        private final ViewTreeObserver.OnGlobalFocusChangeListener mGlobalFocusChangeListener =
                new ViewTreeObserver.OnGlobalFocusChangeListener() {
                    @Override
                    public void onGlobalFocusChanged(View oldFocus, View newFocus) {
                        onChildFocus(
                                GuideUtils.isDescendant(mContainer, oldFocus) ? oldFocus : null,
                                GuideUtils.isDescendant(mContainer, newFocus) ? newFocus : null);
                    }
                };


        // Members of Channel Header
        private Channel mChannel;
        private final TextView mChannelNameView;

        private boolean mIsInputLogoVisible;
        private AccessibilityStateChangeListener mAccessibilityStateChangeListener =
                new AccessibilityStateChangeListener() {
                    @Override
                    public void onAccessibilityStateChanged(boolean enable) {
                        enable &= !CommonUtils.isRunningInTest();
                    }
                };

        ProgramRowViewHolder(View itemView) {
            super(itemView);

            mContainer = (ViewGroup) itemView;
            mContainer.addOnAttachStateChangeListener(
                    new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            mContainer
                                    .getViewTreeObserver()
                                    .addOnGlobalFocusChangeListener(mGlobalFocusChangeListener);
                            mAccessibilityManager.addAccessibilityStateChangeListener(
                                    mAccessibilityStateChangeListener);
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mContainer
                                    .getViewTreeObserver()
                                    .removeOnGlobalFocusChangeListener(mGlobalFocusChangeListener);
                            mAccessibilityManager.removeAccessibilityStateChangeListener(
                                    mAccessibilityStateChangeListener);
                        }
                    });
            mProgramRow = (ProgramRow) mContainer.findViewById(R.id.row);
            mChannelNameView = (TextView) mContainer.findViewById(R.id.channel_name);

            boolean accessibilityEnabled =
                    mAccessibilityManager.isEnabled() && !CommonUtils.isRunningInTest();
        }

        public void onBind(int position) {
//            onBindChannel(mProgramManager.getChannel(position));

            mProgramRow.swapAdapter(mProgramListAdapters.get(position), true);
//            mProgramRow.setProgramGuide(mProgramGuide);
//            mProgramRow.setChannel(mProgramManager.getChannel(position));
            mProgramRow.setChildFocusListener(this);
//            mProgramRow.resetScroll(mProgramGuide.getTimelineRowScrollOffset());


//            // The bottom-left of the last channel header view will have a rounded corner.
//            mChannelHeaderView.setBackgroundResource(
//                    (position < mProgramListAdapters.size() - 1)
//                            ? R.drawable.program_guide_table_header_column_item_background
//                            : R.drawable.program_guide_table_header_column_last_item_background);
        }

        private void onBindChannel(Channel channel) {
            if (DEBUG) LogUtil.d(TAG, "onBindChannel " + channel);

            mChannel = channel;
            mIsInputLogoVisible = false;
            if (channel == null) {
                mChannelNameView.setVisibility(View.GONE);
                return;
            }


            LogUtil.i(this, "ProgramRowViewHolder.onBindChannel.channel.getName:" + channel.getName());
            mChannelNameView.setText(channel.getName());
            mChannelNameView.setVisibility(View.VISIBLE);
        }

        @Override
        public void onChildFocus(View oldFocus, View newFocus) {
            if (newFocus == null) {
                return;
            } // When the accessibility service is enabled, focus might be put on channel's header
            // or
            // detail view, besides program items.

            mSelectedEntry = ((ProgramItemView) newFocus).getTableEntry();

            if (oldFocus == null) {
//                // Focus moved from other row.
//                if (mProgramGuide.getProgramGrid().isInLayout()) {
//                    // We need to post runnable to avoid updating detail view when
//                    // the recycler view is in layout, which may cause detail view not
//                    // laid out according to the updated contents.
//                    mHandler.post(mUpdateDetailViewRunnable);
//                } else {
//                    updateDetailView();
//                }
                return;
            }


        }

        private void updateDetailView() {
            if (mSelectedEntry == null) {
                // The view holder is never on focus before.
                return;
            }
            if (Program.isProgramValid(mSelectedEntry.program)) {
                // mTitleView.setTextColor(mDetailTextColor);
                Context context = itemView.getContext();
                Program program = mSelectedEntry.program;


                String episodeTitle = program.getEpisodeDisplayTitle(mContext);
                if (TextUtils.isEmpty(episodeTitle)) {
                    // mTitleView.setText(program.getTitle());
                } else {
                    String title = program.getTitle();
                    String fullTitle = title + "  " + episodeTitle;

                    SpannableString text = new SpannableString(fullTitle);
                    text.setSpan(
                            mEpisodeTitleStyle,
                            fullTitle.length() - episodeTitle.length(),
                            fullTitle.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    // mTitleView.setText(text);
                }

            }
        }


        // The return value of this method will indicate the target view is visible (true)
        // or gone (false).
        private boolean updateTextView(TextView textView, String text) {
            if (!TextUtils.isEmpty(text)) {
                textView.setVisibility(View.VISIBLE);
                LogUtil.i(this, "ProgramRowViewHolder.updateTextView.text:" + text);
                textView.setText(text);
                return true;
            } else {
                textView.setVisibility(View.GONE);
                return false;
            }
        }

        private void onHorizontalScrolled() {
            if (mDetailInAnimator != null) {
                mHandler.removeCallbacks(mDetailInStarter);
                mHandler.postDelayed(mDetailInStarter, mAnimationDuration);
            }
        }
    }

}
