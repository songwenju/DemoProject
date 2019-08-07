package com.xiaomi.demoproject;

import android.content.Context;
import android.os.Bundle;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ItemBridgeAdapter;
import android.support.v17.leanback.widget.VerticalGridView;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.FocusFinder;
import android.widget.RelativeLayout;

import com.xiaomi.demoproject.Adapter.RecyclerPresenter;
import com.xiaomi.demoproject.EPG.Channel;
import com.xiaomi.demoproject.EPG.Program;

import java.util.ArrayList;
import java.util.List;

public class RecycleDemoActivity extends AppCompatActivity {
	private List<Channel> mAllChannelList = new ArrayList<>();
	private List<Channel> mChannelList = new ArrayList<>();
	private Context mContext;
	private ArrayObjectAdapter mChannelArrayAdapter;
	private int mPageNum = 0;
	//每页几个内容
	public static final int EPG_CHANNEL_NUM = 4;
	public int currentIndex = 1;
	private VerticalGridView mChannelRecyclerView;
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.recylcer_layout);
		initView();
		initData();
	}

	private void initView() {
		mContext = this;
		mBaseLayout = findViewById(R.id.base_layout);
		mChannelRecyclerView = findViewById(R.id.channel_name_view);
		mPresenter = new RecyclerPresenter(mContext);
		mChannelArrayAdapter = new ArrayObjectAdapter(mPresenter);
		ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(mChannelArrayAdapter);
		mChannelRecyclerView.setAdapter(bridgeAdapter);
	}

	private void initData() {
		for (int i = 0; i < ALL; i++) {
		    Channel channel = new Channel("channel:"+i);
			List<Program> programList = new ArrayList<>();
            for (int j = 0; j < 20;j++){
                Program program = new Program("program:"+j);
                programList.add(program);
            }

            channel.setProgramList(programList);
            mAllChannelList.add(channel);
        }

		mMaxPage = ALL / EPG_CHANNEL_NUM;
		//在第几页
		mPageNum = currentIndex / EPG_CHANNEL_NUM;
		LogUtil.d(this, "RecycleDemoActivity.initData.pageNum:" + mPageNum + ",maxPage:" + mMaxPage);
		getChannel(mPageNum, INIT_CHANNEL);
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

				mChannelRecyclerView.setSelectedPosition(relativeIndex);
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
				mChannelList.removeAll(mChannelList.subList(0,EPG_CHANNEL_NUM));
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
				int temp = Math.min((pageNum + 4) * EPG_CHANNEL_NUM, mAllChannelList.size())- (pageNum + 3) * EPG_CHANNEL_NUM;
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
		int focusIndex = mChannelRecyclerView.getSelectedPosition();
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
