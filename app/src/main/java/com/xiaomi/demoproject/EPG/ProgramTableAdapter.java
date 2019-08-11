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

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.RecycledViewPool;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;

import com.xiaomi.demoproject.LogUtil;
import com.xiaomi.demoproject.R;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProgramTableAdapter extends RecyclerView.Adapter<ProgramTableAdapter.ProgramRowViewHolder> {
    private static final String TAG = "ProgramTableAdapter";
    private final Context mContext;
    private final ProgramManager mProgramManager;
    //横向的也是 item里面有一个recycleView
    private final List<ProgramListAdapter> mProgramListAdapters = new ArrayList<>();
    private final RecycledViewPool mRecycledViewPool;

    public ProgramTableAdapter(Context context, ProgramManager programManager) {
        LogUtil.i(this, "ProgramTableAdapter.ProgramTableAdapter");
        mContext = context;
        mProgramManager = programManager;
        mRecycledViewPool = new RecycledViewPool();
        mRecycledViewPool.setMaxRecycledViews(R.layout.program_guide_table_item,
                context.getResources().getInteger(R.integer.max_recycled_view_pool_epg_table_item));
        mProgramManager.addListener(
                new ProgramManager.ListenerAdapter() {
                    @Override
                    public void onChannelsUpdated() {
                        update();
                    }
                });
        update();
    }

    private void update() {
        mProgramListAdapters.clear();
        LogUtil.i(this,"ProgramTableAdapter.update.childCount:"+mProgramManager.getChannelCount());
        for (int i = 0; i < mProgramManager.getChannelCount(); i++) {
            ProgramListAdapter listAdapter =
                    new ProgramListAdapter(mContext.getResources(), mProgramManager, i);
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
        LogUtil.i(this,"ProgramTableAdapter.onBindViewHolder");
        holder.onBind(position);
    }

    @Override
    public void onBindViewHolder(ProgramRowViewHolder holder, int position, List<Object> payloads) {
        super.onBindViewHolder(holder, position, payloads);
    }

    @Override
    public ProgramRowViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
        ProgramRow programRow = (ProgramRow) itemView.findViewById(R.id.row);
        programRow.setRecycledViewPool(mRecycledViewPool);
        return new ProgramRowViewHolder(itemView);
    }


    class ProgramRowViewHolder extends RecyclerView.ViewHolder
            implements ProgramRow.ChildFocusListener {
        private final ViewGroup mContainer;
        private final ProgramRow mProgramRow;

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


        private final TextView mChannelNameView;
        ProgramRowViewHolder(View itemView) {
            super(itemView);
            mContainer = (ViewGroup) itemView;
            mContainer.addOnAttachStateChangeListener(
                    new View.OnAttachStateChangeListener() {
                        @Override
                        public void onViewAttachedToWindow(View v) {
                            mContainer.getViewTreeObserver()
                                    .addOnGlobalFocusChangeListener(mGlobalFocusChangeListener);
                        }

                        @Override
                        public void onViewDetachedFromWindow(View v) {
                            mContainer
                                    .getViewTreeObserver()
                                    .removeOnGlobalFocusChangeListener(mGlobalFocusChangeListener);
                        }
                    });
            mProgramRow = (ProgramRow) mContainer.findViewById(R.id.row);
            mChannelNameView = (TextView) mContainer.findViewById(R.id.channel_name);

        }

        public void onBind(int position) {
            LogUtil.i(this,"ProgramRowViewHolder.onBind");
            onBindChannel(mProgramManager.getChannel(position));

            mProgramRow.swapAdapter(mProgramListAdapters.get(position), true);
            mProgramRow.setChannel(mProgramManager.getChannel(position));
            mProgramRow.setChildFocusListener(this);
//            mProgramRow.resetScroll(mProgramGuide.getTimelineRowScrollOffset());


//            // The bottom-left of the last channel header view will have a rounded corner.
//            mChannelHeaderView.setBackgroundResource(
//                    (position < mProgramListAdapters.size() - 1)
//                            ? R.drawable.program_guide_table_header_column_item_background
//                            : R.drawable.program_guide_table_header_column_last_item_background);
        }

        private void onBindChannel(Channel channel) {
            LogUtil.d(TAG, "onBindChannel channel:" + channel);
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
            LogUtil.i(this, "ProgramRowViewHolder.onChildFocus");
            if (newFocus == null) {
                return;
            } // When the accessibility service is enabled, focus might be put on channel's header
            // or
            // detail view, besides program items.

//            mSelectedEntry = ((ProgramItemView) newFocus).getTableEntry();

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


        private void onHorizontalScrolled() {

        }
    }

}
