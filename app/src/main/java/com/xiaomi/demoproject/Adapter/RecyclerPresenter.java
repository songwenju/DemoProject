package com.xiaomi.demoproject.Adapter;

import android.content.Context;
import android.support.annotation.NonNull;

import android.support.v17.leanback.widget.Presenter;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

//import com.open.leanback.widget.Presenter;
import com.xiaomi.demoproject.EPG.Channel;
import com.xiaomi.demoproject.EPG.Program;
import com.xiaomi.demoproject.LogUtil;
import com.xiaomi.demoproject.R;

import java.util.List;


public class RecyclerPresenter extends Presenter {
    private Context mContext;

    public RecyclerPresenter(Context context) {
        mContext = context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup viewGroup) {
        return new RecyclerViewHolder(LayoutInflater.from(mContext).inflate(R.layout.program_guide_table_row, null));
    }

    @Override
    public void onBindViewHolder(ViewHolder viewHolder, Object o) {
        if (o instanceof Channel) {
            Channel channel = (Channel) o;
            RecyclerViewHolder recyclerViewHolder = (RecyclerViewHolder) viewHolder;
            recyclerViewHolder.mText.setText(channel.getName());


            LinearLayoutManager layoutManager = new LinearLayoutManager(mContext);
            //设置布局管理器
            recyclerViewHolder.programRecyclerView.setLayoutManager(layoutManager);
            //设置为横向布局
            layoutManager.setOrientation(OrientationHelper.HORIZONTAL);

            //设置Adapter
            recyclerViewHolder.programRecyclerView.setAdapter(new ProgramAdapter(channel.getProgramList()));
            //设置分隔线
//            recyclerViewHolder.programRecyclerView.addItemDecoration(new DividerGridItemDecoration(mContext));
            //设置增加或删除条目的动画
            recyclerViewHolder.programRecyclerView.setItemAnimator(new DefaultItemAnimator());

        }
    }

    @Override
    public void onUnbindViewHolder(ViewHolder viewHolder) {
        LogUtil.i(this, "RecyclerPresenter.onUnbindViewHolder");
    }

    private class RecyclerViewHolder extends Presenter.ViewHolder {
        private TextView mText;
        private RecyclerView programRecyclerView;

        public RecyclerViewHolder(@NonNull View itemView) {
            super(itemView);
            mText = itemView.findViewById(R.id.channel_name);
            programRecyclerView = itemView.findViewById(R.id.row);
        }
    }


    class ProgramAdapter extends RecyclerView.Adapter{
        private List<Program> mProgramList;

        public ProgramAdapter(List<Program> programList) {
            mProgramList = programList;
            LogUtil.i(this,"ProgramAdapter.ProgramAdapter.programList:"+programList);
        }

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
            return new ProgramViewHolder(LayoutInflater.from(mContext).inflate(R.layout.program_item, null));
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int i) {

            if (viewHolder instanceof  ProgramViewHolder){
                ProgramViewHolder programViewHolder = (ProgramViewHolder) viewHolder;
                LogUtil.i(this,"ProgramAdapter.onBindViewHolder.name:"+mProgramList.get(i).getTitle());
                programViewHolder.mTextView.setText(mProgramList.get(i).getTitle());
            }

        }

        @Override
        public int getItemCount() {

            LogUtil.i(this,"ProgramAdapter.getItemCount.size:"+(mProgramList == null ? 0: mProgramList.size()));
            return mProgramList == null ? 0: mProgramList.size();
        }
    }


    class ProgramViewHolder extends RecyclerView.ViewHolder{
        private TextView mTextView;

        public ProgramViewHolder(@NonNull View itemView) {
            super(itemView);
            mTextView = itemView.findViewById(R.id.program_item_text);
        }
    }
}
