package com.xiaomi.demoproject;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.OnChildSelectedListener;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.RecyclerView;
import android.view.KeyEvent;
import android.view.View;
import android.view.FocusFinder;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.xiaomi.demoproject.Adapter.RecyclerPresenter;
import com.xiaomi.demoproject.EPG.Channel;
import com.xiaomi.demoproject.EPG.GuideUtils;
import com.xiaomi.demoproject.EPG.Program;
import com.xiaomi.demoproject.EPG.ProgramGridView;
import com.xiaomi.demoproject.EPG.ProgramManager;
import com.xiaomi.demoproject.EPG.ProgramTableAdapter;
import com.xiaomi.demoproject.EPG.TimeListAdapter;
import com.xiaomi.demoproject.EPG.TimelineRow;
import com.xiaomi.demoproject.EPG.TvClock;
import com.xiaomi.demoproject.util.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RecycleDemoActivity extends AppCompatActivity {
    private List<Channel> mAllChannelList = new ArrayList<>();
    private List<Channel> mChannelList = new ArrayList<>();
    private Context mContext;
    private ArrayObjectAdapter mChannelArrayAdapter;
    private int mPageNum = 0;
    //每页几个内容
    public static final int EPG_CHANNEL_NUM = 4;
    public int currentIndex = 1;
    //    private VerticalGridView mProgramGridView;
    private int qHead = 0;
    private int qTail = 0;
    public static final int INIT_CHANNEL = 0;
    public static final int DOWN_CHANNEL = 1;
    public static final int UP_CHANNEL = 2;
    public static final int ALL = 50;
    private int mChildCount = 0;
    private int mMaxPage;
    private RecyclerPresenter mPresenter;
    private RelativeLayout mBaseLayout;
    private ArrayList<Integer> mLoadedPageList = new ArrayList<>();


    private TimelineRow mTimelineRow;
    private TimeListAdapter mTimeListAdapter;
    private ProgramGridView mProgramGridView;
    private ProgramManager mProgramManager;
    private long mStartUtcTime;
    private TvClock mClock;
    private int mWidthPerHour;

    private static final long HOUR_IN_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final long HALF_HOUR_IN_MILLIS = HOUR_IN_MILLIS / 2;
    private static final long MIN_DURATION_FROM_START_TIME_TO_CURRENT_TIME =
            ProgramManager.FIRST_ENTRY_MIN_DURATION;

    private long mViewPortMillis;
    private boolean mTimelineAnimation = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.recylcer_layout);
        initView();
        initData();
        show();

    }

    private final ProgramManagerListener mProgramManagerListener = new ProgramManagerListener();

    private class ProgramManagerListener extends ProgramManager.ListenerAdapter {
        @Override
        public void onTimeRangeUpdated() {
            int scrollOffset =
                    (int) (mWidthPerHour * mProgramManager.getShiftedTime() / HOUR_IN_MILLIS);

            LogUtil.d(this, "Horizontal scroll to "
                            + scrollOffset
                            + " pixels ("
                            + mProgramManager.getShiftedTime()
                            + " millis)");

            mTimelineRow.scrollTo(scrollOffset, mTimelineAnimation);
        }
    }

    private void show() {

    }

    private void initView() {
        mContext = this;
        mBaseLayout = findViewById(R.id.base_layout);
        mChannelArrayAdapter = new ArrayObjectAdapter(mPresenter);

        Resources res = mContext.getResources();
        mTimelineRow = (TimelineRow) findViewById(R.id.time_row);
        mTimeListAdapter = new TimeListAdapter(res);
//        这里的第一个参数是viewType，在adapter里使用
        mTimelineRow
                .getRecycledViewPool()
                .setMaxRecycledViews(
                        R.layout.program_guide_table_header_row_item,
                        res.getInteger(R.integer.max_recycled_view_pool_epg_header_row_item));
        mTimelineRow.setAdapter(mTimeListAdapter);
        mClock = new TvClock(mContext);

        mProgramManager = new ProgramManager();

        //设置每小时宽度
        mWidthPerHour = res.getDimensionPixelSize(R.dimen.program_guide_table_width_per_hour);
        GuideUtils.setWidthPerHour(mWidthPerHour);
        Utils.setWidthPerHour(mWidthPerHour);

        //init grid
        mProgramGridView = findViewById(R.id.channel_name_view);
        mProgramGridView.initialize(mProgramManager);
        mProgramGridView.getRecycledViewPool()
                .setMaxRecycledViews(
                        R.layout.program_guide_table_row,
                        res.getInteger(R.integer.max_recycled_view_pool_epg_table_row));
        ProgramTableAdapter programTableAdapter = new ProgramTableAdapter(mContext, mProgramManager);

        mProgramGridView.setAdapter(programTableAdapter);
        mProgramGridView.setChildFocusListener(new ProgramGridView.ChildFocusListener() {
            @Override
            public void onRequestChildFocus(View oldFocus, View newFocus) {

            }
        });
        mProgramGridView.setOnChildSelectedListener(
                new OnChildSelectedListener() {
                    @Override
                    public void onChildSelected(
                            ViewGroup parent, View view, int position, long id) {
//                        selectRow(view);
                    }
                });
        mProgramGridView.setFocusScrollStrategy(ProgramGridView.FOCUS_SCROLL_ALIGNED);
//        mProgramGridView.setWindowAlignmentOffset(mSelectionRow * mRowHeight);
//        mProgramGridView.setWindowAlignmentOffsetPercent(ProgramGrid.WINDOW_ALIGN_OFFSET_PERCENT_DISABLED);
        mProgramGridView.setItemAlignmentOffset(0);
        mProgramGridView.setItemAlignmentOffsetPercent(ProgramGridView.ITEM_ALIGN_OFFSET_PERCENT_DISABLED);

        RecyclerView.OnScrollListener onScrollListener =
                new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        onHorizontalScrolled(dx);
                    }
                };
        mTimelineRow.addOnScrollListener(onScrollListener);


//        mStartUtcTime =
//                Utils.floorTime(
//                        mClock.currentTimeMillis() - MIN_DURATION_FROM_START_TIME_TO_CURRENT_TIME,
//                        HALF_HOUR_IN_MILLIS);

        mStartUtcTime = 1561401000000L;
        mProgramManager.updateInitialTimeRange(mStartUtcTime, mStartUtcTime + mViewPortMillis);
        mProgramManager.addListener(mProgramManagerListener);

        mTimeListAdapter.update(mStartUtcTime);
        mTimelineRow.resetScroll();
    }


    private void onHorizontalScrolled(int dx) {
        LogUtil.d(this, "onHorizontalScrolled(dx=" + dx + ")");
//		positionCurrentTimeIndicator();
        for (int i = 0, n = mProgramGridView.getChildCount(); i < n; ++i) {
            mProgramGridView.getChildAt(i).findViewById(R.id.row).scrollBy(dx, 0);
        }
    }

    private void initData() {
        mAllChannelList.clear();
        mAllChannelList.addAll(mProgramManager.getChannels());

        mMaxPage = ALL / EPG_CHANNEL_NUM;
        //在第几页
        mPageNum = currentIndex / EPG_CHANNEL_NUM;
        LogUtil.d(this, "RecycleDemoActivity.initData.pageNum:" + mPageNum + ",maxPage:" + mMaxPage);
        getChannel(mPageNum, INIT_CHANNEL);

        Resources res = getResources();
        Point displaySize = new Point();
        getWindowManager().getDefaultDisplay().getSize(displaySize);
        int gridWidth =
                displaySize.x
                        - res.getDimensionPixelOffset(R.dimen.program_guide_table_margin_start)
                        - res.getDimensionPixelSize(
                        R.dimen.program_guide_table_header_column_width);
        mViewPortMillis = (gridWidth * HOUR_IN_MILLIS) / mWidthPerHour;

    }

    private void getChannel(int pageNum, int getType) {
        int startPosition;
        int endPosition;
        LogUtil.i(this, "RecycleDemoActivity.getChannel,qHead:" + qHead + ",qTail:" + qTail);

        switch (getType) {
            case INIT_CHANNEL:
                //队尾添加list
                if (pageNum == 0) {
                    startPosition = pageNum * EPG_CHANNEL_NUM;
                    endPosition = (pageNum + 3) * EPG_CHANNEL_NUM;
                    mLoadedPageList.add(0);
                    mLoadedPageList.add(1);
                    mLoadedPageList.add(2);
                } else if (pageNum == mMaxPage) {
                    startPosition = (pageNum - 2) * EPG_CHANNEL_NUM;
                    endPosition = (pageNum + 1) * EPG_CHANNEL_NUM;
                    mLoadedPageList.add(pageNum - 2);
                    mLoadedPageList.add(pageNum - 1);
                    mLoadedPageList.add(pageNum);
                } else {
                    startPosition = (pageNum - 1) * EPG_CHANNEL_NUM;
                    endPosition = (pageNum + 2) * EPG_CHANNEL_NUM;
                    mLoadedPageList.add(pageNum - 1);
                    mLoadedPageList.add(pageNum);
                    mLoadedPageList.add(pageNum + 1);
                }
                startPosition = Math.max(0, startPosition);
                endPosition = Math.min(endPosition, ALL);
                mChildCount = EPG_CHANNEL_NUM * 3;
                mChannelList.addAll(0, mAllChannelList.subList(startPosition, endPosition));

                int relativeIndex;
                mChannelArrayAdapter.addAll(0, mChannelList);
                if (pageNum == 0 || mMaxPage < 3) {
                    relativeIndex = currentIndex;
                } else {
                    if (pageNum == mMaxPage) {
                        relativeIndex = currentIndex - (mPageNum - 2) * EPG_CHANNEL_NUM;
                    } else {
                        relativeIndex = currentIndex - (mPageNum - 1) * EPG_CHANNEL_NUM;
                    }
                }

                mProgramGridView.setSelectedPosition(relativeIndex);
                LogUtil.d(this, "RecycleDemoActivity.getChannel.mChannelList.init:" + mChannelList);
                LogUtil.d(this, "RecycleDemoActivity.getChannel.mLoadedPageList:" + mLoadedPageList);
                break;
            case DOWN_CHANNEL:
                //队头删除一页，队尾添加一页
                //方案1
                startPosition = pageNum * EPG_CHANNEL_NUM;
                endPosition = (pageNum + 1) * EPG_CHANNEL_NUM;
                startPosition = Math.min(startPosition, mAllChannelList.size());
                endPosition = Math.min(endPosition, mAllChannelList.size());
                mLoadedPageList.remove(Integer.valueOf(pageNum - 3));
                mLoadedPageList.add(pageNum);
                mChannelList.removeAll(mChannelList.subList(0, EPG_CHANNEL_NUM));
                mChannelList.addAll(mChannelList.size(), mAllChannelList.subList(startPosition, endPosition));

                mChannelArrayAdapter.addAll(mChildCount, mAllChannelList.subList(startPosition, endPosition));
                mChannelArrayAdapter.removeItems(0, EPG_CHANNEL_NUM);
                LogUtil.d(this, "RecycleDemoActivity.getChannel.mChannelList.down:" + mChannelList);
                LogUtil.d(this, "RecycleDemoActivity.getChannel.down.mLoadedPageList:" + mLoadedPageList);
                LogUtil.i(this, "RecycleDemoActivity.down.startPosition:" + startPosition + ",endPosition:" + endPosition);
                break;
            case UP_CHANNEL:
                //队头添加一页，队尾删除一页
                startPosition = pageNum * EPG_CHANNEL_NUM;
                //方案1
                endPosition = (pageNum + 1) * EPG_CHANNEL_NUM;
                startPosition = Math.max(startPosition, 0);
                endPosition = Math.max(endPosition, 0);
                mLoadedPageList.add(pageNum);
                mLoadedPageList.remove(Integer.valueOf(pageNum + 3));
                LogUtil.d(this, "RecycleDemoActivity.getChannel.up.mLoadedPageList:" + mLoadedPageList);
                mChannelList.addAll(0, mAllChannelList.subList(startPosition, endPosition));

                LogUtil.i(this, "RecycleDemoActivity.down.startPosition:" + startPosition + ",endPosition:" + endPosition);
                int temp = Math.min((pageNum + 4) * EPG_CHANNEL_NUM, mAllChannelList.size()) - (pageNum + 3) * EPG_CHANNEL_NUM;
                mChannelList.removeAll(mChannelList.subList(mChildCount, mChildCount + temp));

                mChannelArrayAdapter.addAll(0, new ArrayList<>(mAllChannelList.subList(startPosition, endPosition)));
                mChannelArrayAdapter.removeItems(mChildCount, EPG_CHANNEL_NUM);

                //最后一页一共有多少数据

                LogUtil.d(this, "RecycleDemoActivity.getChannel.mChannelList.up:" + mChannelList);
                break;
        }

        qHead = 0;
        qTail = mChildCount;
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        View focusView;
        int focusIndex = mProgramGridView.getSelectedPosition();
        switch (event.getKeyCode()) {
            case KeyEvent.KEYCODE_DPAD_DOWN:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    return true;
                } else {
                    LogUtil.i(this, "RecycleDemoActivity.dispatchKeyEvent.focusIndex:" + focusIndex);
                    LogUtil.i(this, "RecycleDemoActivity.dispatchKeyEvent.mChildCount:" + mChildCount);
                    if (focusIndex >= mChildCount / 2) {
                        if (mPageNum == 0) {
                            mPageNum = mPageNum + 2;
                        } else {
                            ++mPageNum;
                        }
                        //当前的pageNum，没有load的再load
                        if (mPageNum <= mMaxPage) {
                            if (!mLoadedPageList.contains(mPageNum)) {
                                LogUtil.d(this, "RecycleDemoActivity.dispatchKeyEvent.down.loadMore.mPageNum:" + mPageNum);
                                //卡顿了一下，让焦点的view去查找上下view
                                focusView = getCurrentFocus();
                                View downView = FocusFinder.getInstance().findNextFocus(mBaseLayout, focusView, View.FOCUS_DOWN);
                                if (downView != null) {
                                    downView.requestFocus();
                                }
                                getChannel(mPageNum, DOWN_CHANNEL);
                                return true;
                            }
                        } else {
                            mPageNum = mMaxPage;
                        }
                    }
                }
                break;

            case KeyEvent.KEYCODE_DPAD_UP:
                if (event.getAction() == KeyEvent.ACTION_UP) {
                    return true;
                } else {
                    if (focusIndex <= mChildCount / 2) {
                        --mPageNum;
                        LogUtil.i(this, "RecycleDemoActivity.dispatchKeyEvent.mPageNum:" + mPageNum);
                        if (mPageNum >= 0) {
                            if (!mLoadedPageList.contains(mPageNum)) {
                                LogUtil.d(this, "RecycleDemoActivity.dispatchKeyEvent.up.loadMore.mPageNum:" + mPageNum);
                                focusView = getCurrentFocus();
                                View upView = FocusFinder.getInstance().findNextFocus(mBaseLayout, focusView, View.FOCUS_UP);
                                if (upView != null) {
                                    upView.requestFocus();
                                }
                                getChannel(mPageNum, UP_CHANNEL);
                                return true;
                            }
                        } else {
                            mPageNum = 0;
                        }
                    }
                }
                break;
        }


        return super.dispatchKeyEvent(event);
    }
}
