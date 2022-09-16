package cn.yinxm.media.ms;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

public class DemoAdapter extends RecyclerView.Adapter<DemoAdapter.ViewHolder> {

    private Context context;
    private List<MediaBrowserCompat.MediaItem> list;
    private OnItemClickListener mOnItemClickListener;

    class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle;

        public ViewHolder(View view) {
            super(view);
            textTitle = view.findViewById(R.id.text_title);
        }
    }

    public DemoAdapter(Context context, List<MediaBrowserCompat.MediaItem> list) {
        this.context = context;
        this.list = list;
    }

    public interface OnItemClickListener {
        void onItemClick(View view, int position);

        void onItemLongClick(View view, int position);
    }

    public void setOnItemClickListener(OnItemClickListener mOnItemClickListener) {
        this.mOnItemClickListener = mOnItemClickListener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        int itemViewId = R.layout.item_music;
        ViewHolder holder = new ViewHolder(LayoutInflater.from(context).inflate(itemViewId, parent, false));
        return holder;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        // 绑定组件的事件
        holder.textTitle.setText(list.get(position).getDescription().getTitle());

        // 如果设置了回调，则设置点击事件
        if (mOnItemClickListener != null) {
            holder.itemView.setOnClickListener(v -> {
                Log.d(TAG, "Clicked");
                int pos = holder.getLayoutPosition();
                mOnItemClickListener.onItemClick(holder.itemView, pos);
            });

            //设置长按事件
            holder.itemView.setOnLongClickListener(v -> {
                Log.d(TAG, "LongClicked");
                int pos = holder.getLayoutPosition();
                mOnItemClickListener.onItemLongClick(holder.itemView, pos);
                return true;
            });
        }
    }

    @Override
    public int getItemCount() {
        return list.size();
    }
}
