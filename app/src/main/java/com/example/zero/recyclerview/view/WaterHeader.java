package com.example.zero.recyclerview.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.example.zero.recyclerview.R;
import com.example.zero.recyclerview.view.progressindicator.AVLoadingIndicatorView;


/**
 * 水滴动画
 */
public class WaterHeader extends LinearLayout implements BaseRefreshHeader {
    private RelativeLayout mContainer;
    private WaterDropView mWaterDropView;
    private SimpleViewSwitcher mProgressBar;
    private int mState = STATE_NORMAL;
    private Context mContext;
    private float percent = -1;

    private boolean isRefreshing = false;//是否正在刷新,STATE_REFRESHING被用作将要刷新的状态标识


    private final int ROTATE_ANIM_DURATION = 180;

    public int mMeasuredHeight;//布局的原始高度，用于做状态改变的标志

    public WaterHeader(Context context) {
        super(context);
        initView(context);
    }

    public WaterHeader(Context context, AttributeSet attrs) {
        super(context, attrs);
        initView(context);
    }

    private void initView(Context context) {
        mContext = context;
        // 初始情况，设置下拉刷新view高度为0
        mContainer = (RelativeLayout) LayoutInflater.from(context).inflate(
                R.layout.v_res_recycler_header, null);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 0);
        this.setLayoutParams(lp);
        this.setPadding(0, 0, 0, 0);

        addView(mContainer, new LayoutParams(LayoutParams.MATCH_PARENT, 0));
        setGravity(Gravity.BOTTOM);

        mWaterDropView = (WaterDropView) findViewById(R.id.waterdroplist_waterdrop);

        //initListener the progress view
        mProgressBar = (SimpleViewSwitcher) findViewById(R.id.listview_header_progressbar);
        AVLoadingIndicatorView progressView = new AVLoadingIndicatorView(context);
        progressView.setIndicatorColor(0xffB5B5B5);
        progressView.setIndicatorId(ProgressStyle.BallSpinFadeLoader);
        mProgressBar.setView(progressView);

        measure(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mMeasuredHeight = getMeasuredHeight();
    }

    //外部调用，向下移动显示多少
    @Override
    public void onMove(float delta) {
        if (getVisibleHeight() > 0 || delta > 0) {
            setVisiableHeight((int) delta + getVisibleHeight());
            if (mState <= STATE_RELEASE_TO_REFRESH) { // 未处于刷新状态,进行水滴的拉伸的变化
                /**此处进行水滴下拉变化*/
                //计算显示比例 -- 减去直径，距离顶部的距离
                percent = (getVisibleHeight() - 2 * mWaterDropView.getTopCircle().getRadius() - 30) / mMeasuredHeight;
                mWaterDropView.updateComleteState(percent);
                if (percent >= 1) {//完成度大于等于1
                    setState(STATE_RELEASE_TO_REFRESH);
                } else {
                    setState(STATE_NORMAL);
                }
            }
        }
    }

    //根据状态改变 控件显示 和 刷新动作
    public void setState(int state) {
        if (state == mState) return;
        switch (state) {
            case STATE_NORMAL://正常状态(4)
                mProgressBar.setVisibility(View.INVISIBLE);
                mWaterDropView.setVisibility(View.VISIBLE);
                break;
            case STATE_RELEASE_TO_REFRESH://释放刷新(1)
                mProgressBar.setVisibility(View.INVISIBLE);
                mWaterDropView.setVisibility(View.VISIBLE);
                break;
            case STATE_REFRESHING://放手了，正在刷新...(2)
                //进行动画回弹之后显示进度条
                ValueAnimator reboundAnima = mWaterDropView.createReboundAnima(500);
                reboundAnima.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        mProgressBar.setVisibility(View.VISIBLE);
                        mWaterDropView.setVisibility(View.INVISIBLE);
                    }
                });
                reboundAnima.start();
                break;
            case STATE_DONE://刷新完成(3)
                mProgressBar.setVisibility(View.INVISIBLE);
                mWaterDropView.setVisibility(View.INVISIBLE);
                break;
            default:
        }
        mState = state;
    }


    //完成刷新：改变状态设置动作 -- 外部调用
    @Override
    public void refreshComplete() {
        new Handler().postDelayed(new Runnable() {
            public void run() {
                setState(STATE_DONE);//先设置：STATE_DONE
                reset();//这里面有方法设置为：STATE_NORMAL
                isRefreshing = false;
            }
        }, 500);
    }

    //改变头部可见的高度
    public void setVisiableHeight(int height) {
        if (height < 0)
            height = 0;
        LayoutParams lp = (LayoutParams) mContainer
                .getLayoutParams();
        lp.height = height;
        mContainer.setLayoutParams(lp);
    }

    public int getVisibleHeight() {
        int height = 0;
        LayoutParams lp = (LayoutParams) mContainer
                .getLayoutParams();
        height = lp.height;
        return height;
    }

    //外部调用判断是否满足刷新条件,进行刷新动作的执行
    @Override
    public boolean releaseAction() {
        boolean isOnRefresh = false;
        int height = getVisibleHeight();
        if (height == 0) // not visible.
            isOnRefresh = false;
//        if (getVisibleHeight() > mMeasuredHeight && mState < STATE_REFRESHING) {//改成通过水滴完成度判断
        if (percent >= 1 && mState < STATE_REFRESHING) {
            setState(STATE_REFRESHING);
            isOnRefresh = true;
        }
        // refreshing and header isn't shown fully. do nothing.
        if (mState == STATE_REFRESHING && height <= mMeasuredHeight) {
            //return;
        }
        int destHeight = 0; // default: scroll back to dismiss header.
        // is refreshing, just scroll back to show all the header.
        if (mState == STATE_REFRESHING) {//重复刷新拦截放在外面,这里用于下拉到抵自动刷新
            destHeight = mMeasuredHeight;
            isOnRefresh = true;
        }
        smoothScrollTo(destHeight, false);
        //正在刷新时返回false
        if (isRefreshing)
            isOnRefresh = false;
        //刷新时置为：正在刷新,刷新结束后置为false
        if (isOnRefresh)
            isRefreshing = true;
        return isOnRefresh;
    }

    //重置刷新头部
    public void reset() {
        smoothScrollTo(0, true);
    }

    //地洞到底部之后才设置为正常状态
    private void smoothScrollTo(final int destHeight, final boolean isreset) {
        ValueAnimator animator = ValueAnimator.ofInt(getVisibleHeight(), destHeight);
        animator.setDuration(300).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                setVisiableHeight((int) animation.getAnimatedValue());
                //是重置的情况下才在最后设为：STATE_NORMAL，避免结束时会显示刷新按钮
                if (destHeight == (int) animation.getAnimatedValue() && isreset)
                    setState(STATE_NORMAL);
            }
        });
        animator.start();
    }

    public int getState() {
        return mState;
    }

    /**
     * 进度条类型
     * 选择一个类型的进度条
     */
    public void setProgressStyle(int style) {
        if (style == ProgressStyle.SysProgress) {
            mProgressBar.setView(new ProgressBar(mContext, null, android.R.attr.progressBarStyle));
        } else {
            AVLoadingIndicatorView progressView = new AVLoadingIndicatorView(this.getContext());
            progressView.setIndicatorColor(0xffB5B5B5);
            progressView.setIndicatorId(style);
            mProgressBar.setView(progressView);
        }
    }
}
