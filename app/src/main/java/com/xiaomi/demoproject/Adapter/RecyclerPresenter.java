package com.xiaomi.demoproject.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;

import android.support.v17.leanback.widget.Presenter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

//import com.open.leanback.widget.Presenter;
import com.xiaomi.demoproject.LogUtil;
import com.xiaomi.demoproject.R;


public class RecyclerPresenter extends Presenter {
	private Context mContext;

	public RecyclerPresenter(Context context) {
		mContext = context;
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
		return new RecyclerViewHolder(LayoutInflater.from(mContext).inflate(R.layout.recycler_item, null));
	}

	@Override
	public void onBindViewHolder(ViewHolder viewHolder, Object o) {
		if (o instanceof Integer) {
			Integer integer = (Integer) o;
			RecyclerViewHolder recyclerViewHolder = (RecyclerViewHolder) viewHolder;
			recyclerViewHolder.mText.setText("position:" + integer);
			recyclerViewHolder.mSubText.setText("position:" + integer);
		}
	}

	@Override
	public void onUnbindViewHolder(ViewHolder viewHolder) {
		LogUtil.i(this,"RecyclerPresenter.onUnbindViewHolder");
	}

	private class RecyclerViewHolder extends Presenter.ViewHolder {
		private TextView mText;
		private TextView mSubText;

		public RecyclerViewHolder(@NonNull View itemView) {
			super(itemView);
			mText = itemView.findViewById(R.id.item_text);
			mSubText = itemView.findViewById(R.id.sub_text);
		}
	}
}
