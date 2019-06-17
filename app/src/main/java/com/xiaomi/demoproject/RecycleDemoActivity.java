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

import java.util.ArrayList;
import java.util.List;

public class RecycleDemoActivity extends AppCompatActivity {
	private List<Integer> mAllList = new ArrayList<>();
	private List<Integer> mChannelList = new ArrayList<>();
	private Context mContext;
	private ArrayObjectAdapter mAdapter;
	private int mPageNum = 0;
	public static final int PAGE_NUM = 5;
	public int currentIndex = 15;
	private VerticalGridView mRecyclerView;
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
		mRecyclerView = findViewById(R.id.recycler);
		mPresenter = new RecyclerPresenter(mContext);
		mAdapter = new ArrayObjectAdapter(mPresenter);
		ItemBridgeAdapter bridgeAdapter = new ItemBridgeAdapter(mAdapter);
		mRecyclerView.setAdapter(bridgeAdapter);
	}

	private void initData() {

		for (int i = 0; i < ALL; i++) {
			mAllList.add(i);
		}

		mMaxPage = ALL / PAGE_NUM;
		//在第几页
		mPageNum = currentIndex / PAGE_NUM;
		LogUtil.d(this, "RecycleDemoActivity.initData.pageNum:" + mPageNum + ",maxPage:" + mMaxPage);
		getChannel(mPageNum, INIT_CHANNEL);
	}

	private void getChannel(int pageNum, int getType) {
		int startPosition = 0;
		int endPosition = 0;
		LogUtil.i(this, "RecycleDemoActivity.getChannel,qHead:" + qHead + ",qTail:" + qTail);

		switch (getType) {
			case INIT_CHANNEL:
				//队尾添加list
				if (pageNum == 0) {
					startPosition = pageNum * PAGE_NUM;
					endPosition = (pageNum + 3) * PAGE_NUM;
					mLoadedPageList.add(0);
					mLoadedPageList.add(1);
					mLoadedPageList.add(2);
				} else if (pageNum == mMaxPage) {
					startPosition = (pageNum - 2) * PAGE_NUM;
					endPosition = (pageNum + 1) * PAGE_NUM;
					mLoadedPageList.add(pageNum - 2);
					mLoadedPageList.add(pageNum - 1);
					mLoadedPageList.add(pageNum);
				} else {
					startPosition = (pageNum - 1) * PAGE_NUM;
					endPosition = (pageNum + 2) * PAGE_NUM;
					mLoadedPageList.add(pageNum - 1);
					mLoadedPageList.add(pageNum);
					mLoadedPageList.add(pageNum + 1);
				}
				startPosition = Math.max(0, startPosition);
				endPosition = Math.min(endPosition, ALL);
				mChildCount = PAGE_NUM * 3;
				mChannelList.addAll(0, mAllList.subList(startPosition, endPosition));
				LogUtil.i(this,"RecycleDemoActivity.getChannel.mAllList:"+mAllList);
				mAdapter.addAll(qTail, mAllList.subList(startPosition, endPosition));
//				setTag(mAllList.subList(startPosition, endPosition));
				LogUtil.i(this, "RecycleDemoActivity.initChannel.startPosition:" + startPosition + ",endPosition:" + endPosition);
				if (pageNum == 0) {
					mRecyclerView.setSelectedPosition(currentIndex);
				} else if (pageNum == mMaxPage) {
					mRecyclerView.setSelectedPosition(currentIndex - (mPageNum - 2) * PAGE_NUM);
				} else {
					mRecyclerView.setSelectedPosition(currentIndex - (mPageNum - 1) * PAGE_NUM);
				}
				LogUtil.d(this, "RecycleDemoActivity.getChannel.mChannelList.init:" + mChannelList);
				LogUtil.d(this, "RecycleDemoActivity.getChannel.mLoadedPageList:" + mLoadedPageList);
				break;
			case DOWN_CHANNEL:
				//队头删除一页，队尾添加一页
				//方案1
				startPosition = pageNum * PAGE_NUM;
				endPosition = (pageNum + 1) * PAGE_NUM;
				startPosition = Math.min(startPosition, ALL);
				endPosition = Math.min(endPosition, ALL);
				mAdapter.addAll(qTail, mAllList.subList(startPosition, endPosition));
				mAdapter.removeItems(qHead, PAGE_NUM);

				mChannelList.addAll(qTail, mAllList.subList(startPosition, endPosition));
				LogUtil.i(this, "RecycleDemoActivity.getChannel.subList:" + mChannelList.subList(0, PAGE_NUM));
				mChannelList.removeAll(mChannelList.subList(0, PAGE_NUM));

				mLoadedPageList.remove(Integer.valueOf(pageNum - 3));
				mLoadedPageList.add(pageNum);

				LogUtil.d(this, "RecycleDemoActivity.getChannel.mChannelList.down:" + mChannelList);

				//方案2
//				startPosition = (pageNum - 1) * PAGE_NUM;
//				endPosition = (pageNum + 2) * PAGE_NUM;
//				startPosition = Math.min(startPosition, ALL);
//				endPosition = Math.min(endPosition, ALL);
//				mAdapter.setItems(mAllList.subList(startPosition, endPosition), null);
//				mRecyclerView.setSelectedPosition(3);
//				mPresenter.setFocusPosition(true,3);
				LogUtil.d(this, "RecycleDemoActivity.getChannel.down.mLoadedPageList:" + mLoadedPageList);
				LogUtil.i(this, "RecycleDemoActivity.down.startPosition:" + startPosition + ",endPosition:" + endPosition);
				break;
			case UP_CHANNEL:
				//队头添加一页，队尾删除一页
				startPosition = pageNum * PAGE_NUM;
				//方案1
				endPosition = (pageNum + 1) * PAGE_NUM;
				startPosition = Math.max(startPosition, 0);
				endPosition = Math.max(endPosition, 0);
				LogUtil.i(this, "RecycleDemoActivity.down.startPosition:" + startPosition + ",endPosition:" + endPosition);
				mAdapter.addAll(qHead, mAllList.subList(startPosition, endPosition));
				mAdapter.removeItems(qTail, PAGE_NUM);



				//最后一页一共有多少数据
				int temp = Math.min(qTail + (pageNum + 4) * PAGE_NUM, mAllList.size())- (pageNum + 3) * PAGE_NUM;
				mChannelList.removeAll(mChannelList.subList(qTail, qTail + temp));
				mChannelList.addAll(qHead, mAllList.subList(startPosition, endPosition));
				LogUtil.d(this, "RecycleDemoActivity.getChannel.mChannelList.up:" + mChannelList);


				mLoadedPageList.add(pageNum);
				mLoadedPageList.remove(Integer.valueOf(pageNum + 3));

				//方案2
//				endPosition = (pageNum + 2) * PAGE_NUM;
//				mAdapter.setItems(mAllList.subList(startPosition,endPosition), null);
//				mRecyclerView.setSelectedPosition(11);
				LogUtil.d(this, "RecycleDemoActivity.getChannel.up.mLoadedPageList:" + mLoadedPageList);

				break;
		}

		qHead = 0;
		qTail = mChildCount;
	}


	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		View focusView;
		int focusIndex = mRecyclerView.getSelectedPosition();
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
