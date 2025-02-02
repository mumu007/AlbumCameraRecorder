package com.zhongjh.albumcamerarecorder.widget;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

/**
 * 用于专门服务于app:layout_behavior的，通过自定义view的自定义事件，进行联动
 *
 * @author zhongjh
 * @date 2022/8/11
 */
public class ConstraintLayoutBehavior extends ConstraintLayout {

    public ConstraintLayoutBehavior(Context context) {
        this(context, null);
    }

    public ConstraintLayoutBehavior(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ConstraintLayoutBehavior(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public ConstraintLayoutBehavior(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public Listener getOnListener() {
        return mOnListener;
    }

    public void setOnListener(Listener mOnListener) {
        this.mOnListener = mOnListener;
    }

    /**
     * 点击或长按监听结束后的 确认取消事件监控
     */
    private Listener mOnListener;

    /**
     * 操作按钮的Listener
     */
    public interface Listener {

        /**
         * 触发滑动事件
         *
         * @param translationY 设置Y轴距离
         */
        void onDependentViewChanged(float translationY);
    }

}
