/**
 * @(#) CommonAdapter.java 2016/1/15
 * CopyRight 2015 All rights reserved
 * @modify
 */
package com.example.zero.recyclerview.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.example.zero.recyclerview.view.XRecyclerView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Zero on 2016/7/25.
 */
public abstract class CommomRecyclerAdapter<T> extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    protected Context context;
    protected List<T> mDataSource;
    private XRecyclerView.WrapAdapterDataManager dataManager;

    public void setDataManager(XRecyclerView.WrapAdapterDataManager dataManager) {
        this.dataManager = dataManager;
    }

    public CommomRecyclerAdapter(Context context) {
        this.context = context;
        this.mDataSource = new ArrayList<>();
    }

    public void setDatas(List<T> datas) {
        if (datas == null) datas = new ArrayList<>();
        this.mDataSource.clear();
        this.mDataSource.addAll(datas);
        if (dataManager == null) {
            this.notifyDataSetChanged();
        } else {
            dataManager.notifyDataSetChanged1();
        }
    }


    public void addDatas(List<T> datas) {
        if (datas == null) datas = new ArrayList<>();
        this.mDataSource.addAll(datas);
        if (dataManager == null) {
            this.notifyItemRangeInserted(this.mDataSource.size() - datas.size(), datas.size());
        } else {
            dataManager.notifyItemRangeInserted1(this.mDataSource.size() - datas.size(), datas.size());
        }
    }

    public void addData(T data) {
        if (data == null) return;
        this.mDataSource.add(data);
        if (dataManager == null) {
            this.notifyItemInserted(this.mDataSource.size());
        } else {
            dataManager.notifyItemRangeInserted1(this.mDataSource.size(), 1);
        }
    }

    public void addData(T data, int index) {
        if (data == null) return;
        this.mDataSource.add(index, data);
        if (dataManager == null) {
            this.notifyItemInserted(index);
        } else {
            dataManager.notifyItemRangeInserted1(index, 1);
        }
    }

    public void removeData(T data) {
        int index = this.mDataSource.indexOf(data);
        this.removeData(index);
    }

    public void removeData(int index) {
        if (index == -1) return;
        this.mDataSource.remove(index);
        if (dataManager == null) {
            this.notifyItemRemoved(index);
        } else {
            dataManager.notifyItemRangeRemoved1(index, 1);
        }
    }


    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(onBindViewResource(viewType), parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        onBindView((ViewHolder) holder, getItem(position), position);
    }

    @Override
    public int getItemCount() {
        return mDataSource != null ? mDataSource.size() : 0;
    }

    protected T getItem(int position) {
        if (mDataSource == null) return null;
        if (position >= mDataSource.size()) return null;
        return mDataSource.get(position);
    }

    public abstract void onBindView(ViewHolder mViewHolder, T data, int position);

    public abstract int onBindViewResource(int viewType);

    protected class ViewHolder extends RecyclerView.ViewHolder {
        private SparseArray<View> viewCache;
        private View itemView;

        public ViewHolder(View itemView) {
            super(itemView);
            this.itemView = itemView;
            this.viewCache = new SparseArray<>();
        }

        public <T extends View> T getView(int viewId) {
            View view = viewCache.get(viewId);
            if (view == null) {
                view = this.itemView.findViewById(viewId);
                viewCache.put(viewId, view);
            }
            return (T) view;
        }

        public View getConvertView() {
            return this.itemView;
        }

        public void setText(int textViewResId, String textString) {
        }

        public void setImage(int imageViewResId, String imageUrl) {
        }
    }
}
