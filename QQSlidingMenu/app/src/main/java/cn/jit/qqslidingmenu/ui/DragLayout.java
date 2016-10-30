package cn.jit.qqslidingmenu.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 自定义DragLayout
 */

public class DragLayout extends FrameLayout {

    private ViewDragHelper mDragHelper;
    private ViewGroup mLeftContent;
    private ViewGroup mMainContent;
    private int mWidth; //屏幕宽度
    private int mHeight; //屏幕高度
    private int mRange; //拖拽范围

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    /**
     * 1.实例化ViewDragHelper
     * 参数一：拖拽孩子的父布局，此处即为DragLayout
     */
    private void init() {
        mDragHelper = ViewDragHelper.create(this, mCallback);
    }

    /**
     * 2.将DragLayout的触摸事件传递给ViewDragHelper处理
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return mDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mDragHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //返回true,持续接收事件
        return true;
    }

    /**
     * 3.拿到子View 获取屏幕宽高和设置可拖拽范围(此处为宽度的0.6)
     * 此方法在布局完成后调用,可以拿到子View
     */
    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (getChildCount() < 2) {
            throw new IllegalStateException("Your ViewGroup must have two children at least.");
        }
        if (!(getChildAt(0) instanceof ViewGroup && getChildAt(1) instanceof ViewGroup)) {
            throw new IllegalArgumentException("Your children must be instance of ViewGroup.");
        }
        mLeftContent = (ViewGroup) getChildAt(0);
        mMainContent = (ViewGroup) getChildAt(1);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
        mRange = (int) (mWidth * 0.6);
    }

    /**
     * 4.重写回调事件
     */
    ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {
        //尝试捕获view,返回true表示能被拖拽,false不能被拖拽
        //child表示要拖拽的子view
        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return true;
        }

        //当view被捕获到了(被拖拽了),如果tryCaptureView返回false,则此方法不执行
        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }

        //指定子View的拖拽范围(不会真正生效,只是为了计算动画时间)
        @Override
        public int getViewHorizontalDragRange(View child) {
            return mRange;
        }

        // 根据返回值修正拖拽的位置
        // left child距离屏幕左边的距离
        // left = child.getLeft() + dx;
        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            //下面即规定child只能在0~mRange的范围内拖拽
            if (child == mMainContent) {
                left = fixLeft(left);
            }
            return left;
        }

        //当被拖拽的View位置改变要做的事,比如更新状态、伴随动画、重绘界面
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
            super.onViewPositionChanged(changedView, left, top, dx, dy);
            //如果滑动的是左面板,左面板不动,并把滑动的变化量传递给主面板
            int newLeft = left;
            if (changedView == mLeftContent) {
                newLeft = mMainContent.getLeft() + dx;
            }
            newLeft = fixLeft(newLeft);
            if (changedView == mLeftContent) {
                mLeftContent.layout(0, 0, mWidth, mHeight); //布局左面板
                mMainContent.layout(newLeft, 0, newLeft + mWidth, mHeight); //布局主面板
            }
            float percent = newLeft * 1.0f / mRange; //拖拽距离占总范围的百分比
            //执行动画
            executeAnimation(percent);
            //更新状态
            updateStatus(percent);
            //为了兼容低版本,调用invalidate重绘界面
            invalidate();
        }

        //当被拖拽的view被释放时执行
        //xvel 释放时水平方向的速度
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            super.onViewReleased(releasedChild, xvel, yvel);
            //当释放时没有水平速度并且滑动距离大于mRange的一半 或者有水平向右的速度, 打开左面板
            //否则 其余情况 关闭左面板
            if (xvel == 0 && mMainContent.getLeft() > mRange / 2.0f) {
                open();
            } else if (xvel > 0) {
                open();
            }else {
                close();
            }
        }
    };

    //修正拖拽的范围
    private int fixLeft(int left) {
        if (left < 0) {
            return 0;
        } else if (left > mRange) {
            return mRange;
        }
        return left;
    }

    /*============侧面板打开和关闭部分代码begin=============*/
    public void open() {
        open(true);
    }

    //是否平滑的滑动过去
    public void open(boolean isSmooth) {
        int finalLeft = mRange;
        if (isSmooth) {
            //让主面板平滑打开,方法返回true,表示还没有滑动到指定位置,需要刷新界面
            if (mDragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {
                //刷新界面,参数是child所在的ViewGroup
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, mHeight);
        }
    }

    public void close() {
        close(true);
    }

    public void close(boolean isSmooth) {
        int finalLeft = 0;
        if (isSmooth) {
            //让主面板平滑打开,方法返回true,表示还没有滑动到指定位置,需要刷新界面
            if (mDragHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)) {
                //刷新界面,参数是child所在的ViewGroup
                ViewCompat.postInvalidateOnAnimation(this);
            }
        } else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, mHeight);
        }
    }

    //模板代码,加入即可,保证动画滑动
    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }
    /*============侧面板打开和关闭部分代码end=============*/


    /*============滑动过程中侧面板和主面板执行动画部分代码start=============*/
    //如果没有动画,则和SlidingMenu类似
    private void executeAnimation(float percent) {

        //左面板执行缩放、平移、透明度动画
        mLeftContent.setScaleX(evaluate(percent, 0.5f, 1.0f));
        mLeftContent.setScaleY(evaluate(percent, 0.5f, 1.0f));

        mLeftContent.setTranslationX(evaluate(percent, -mWidth / 2.0f, 0));

        mLeftContent.setAlpha(evaluate(percent, 0.5f, 1.0f));
        //主面板执行缩放动画
        mMainContent.setScaleX(evaluate(percent, 1.0f, 0.8f));
        mMainContent.setScaleY(evaluate(percent, 1.0f, 0.8f));

        //背景动画 从纯黑到透明
        if (getBackground() != null) {
            getBackground().setColorFilter((Integer) evaluateColor(percent, Color.BLACK, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
        }
    }
    /*============滑动过程中侧面板和主面板执行动画部分代码end=============*/

    /*============更新状态部分代码start=============*/
    private Status mCurStatus = Status.Close; //默认状态

    public Status getStatus() {
        return mCurStatus;
    }

    public void setStatus(Status mCurStatus) {
        this.mCurStatus = mCurStatus;
    }

    public static enum Status{
        Close,Open,Draging;
    }
    public interface OnDragStatusChangeListener{
        void onClose();
        void onOpen();
        void onDraging(float percent);
    }
    OnDragStatusChangeListener mListener;
    public void setOnDragStatusChangeListener(OnDragStatusChangeListener listener){
        mListener = listener;
    }
    private void updateStatus(float percent) {
        if (mListener != null){
            mListener.onDraging(percent);
        }
        Status preStatus = mCurStatus;
        mCurStatus = fixStatus(percent);
        if (mCurStatus != preStatus){
            if (mCurStatus == Status.Close){
                if (mListener != null){
                    mListener.onClose();
                }
            }else if (mCurStatus == Status.Open){
                if (mListener != null){
                    mListener.onOpen();
                }
            }
        }
    }
    private Status fixStatus(float percent){
        if (percent == 0f){
            return Status.Close;
        }else if (percent == 1.0f){
            return Status.Open;
        }
        return Status.Draging;
    }
    /*============更新状态部分代码end=============*/

    /**
     * 数值估计值 给一个开始值和结束值 再用给的百分比计算出当前值
     * @param fraction   百分比
     * @param startValue 开始值
     * @param endValue   结束值
     * @return 根据百分比计算出的当前值
     */
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }
    /**
     * 颜色估计值 给一个开始值和结束值 再用给的百分比计算出当前值
     */
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;

        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;

        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }
}
