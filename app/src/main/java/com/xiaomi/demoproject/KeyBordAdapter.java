package com.xiaomi.demoproject;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.Adapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

/**
 * @author songwenju on 18-11-29.
 */
public class KeyBordAdapter extends Adapter {
    private Context mContext;
    private List<Integer> mNumList;
    //圆形
    private static final int ITEM_CIRCLE = 0;
    // 矩形
    private static final int ITEM_RECT = 1;

    public interface OnItemClickListener {
        /**
         * click numbs
         * @param position position
         */
        void onItemClick(int position);

        /**
         * click delete
         */
        void onDeleteClick();
    }

    private OnItemClickListener mOnItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        mOnItemClickListener = onItemClickListener;
    }

    KeyBordAdapter(Context context, List<Integer> numList) {
        LogUtil.i(this, "KeyBordAdapter.KeyBordAdapter");
        this.mContext = context;
        this.mNumList = numList;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == mNumList.size() - 1) {
            return ITEM_RECT;
        } else {
            return ITEM_CIRCLE;
        }
    }


    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        if (viewType == ITEM_RECT) {
            LinearLayout rectView = (LinearLayout) View.inflate(mContext, R.layout.keybord_rect_item, null);
            return new RectRecycleViewHolder(rectView);
        } else {
            View circleView = View.inflate(mContext, R.layout.keybord_circle_item, null);
            return new CircleRecycleViewHolder(circleView);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, final int position) {
        if (viewHolder instanceof RectRecycleViewHolder) {
            RectRecycleViewHolder rectViewHolder = (RectRecycleViewHolder) viewHolder;
            rectViewHolder.rootItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onDeleteClick();
                }
            });
            rectViewHolder.rootItem.setText(mContext.getString(R.string.delete_text));
        } else {
            CircleRecycleViewHolder circleViewHolder = (CircleRecycleViewHolder) viewHolder;
            circleViewHolder.rootItem.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mOnItemClickListener.onItemClick(mNumList.get(position));
                }
            });
            circleViewHolder.rootItem.setText(""+ mNumList.get(position));
            if (position == 0){
                circleViewHolder.rootItem.requestFocus();
            }
        }


    }

    @Override
    public int getItemCount() {
        return mNumList == null ? 0 : mNumList.size();
    }


    class CircleRecycleViewHolder extends RecyclerView.ViewHolder {
        Button rootItem;

        CircleRecycleViewHolder(View itemView) {
            super(itemView);
            rootItem = (Button) itemView.findViewById(R.id.circle_button);
        }
    }

    class RectRecycleViewHolder extends RecyclerView.ViewHolder {
        Button rootItem;
        private LinearLayout layout;

        RectRecycleViewHolder(View itemView) {
            super(itemView);
            layout = (LinearLayout) itemView.findViewById(R.id.rect_layout);
            rootItem = (Button) itemView.findViewById(R.id.rect_button);
        }
    }
}
